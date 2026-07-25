import { NextResponse } from 'next/server';
import { createClient } from '@/lib/supabase/server';

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { text, groupId, clientLogicalDate } = body;

    if (!text || !groupId || !clientLogicalDate) {
      return NextResponse.json({ error: 'Missing text, groupId, or clientLogicalDate' }, { status: 400 });
    }

    const supabase = await createClient();
    
    // Auth check
    const { data: { user }, error: authError } = await supabase.auth.getUser();
    if (authError || !user) {
      // return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }
    
    const userId = user?.id;

    // 1. Robust Regex Parsing (migrated from backend OCR block)
    let hours = 0;
    let minutes = 0;

    // Attempt to find hours (using negative lookahead for letters so "3h45m" works)
    const hrMatch = text.match(/(\d+)\s*(?:h|hr|hrs|hour|hours)(?![a-zA-Z])/i);
    if (hrMatch) {
      hours = parseInt(hrMatch[1], 10);
    }

    // Attempt to find minutes
    const minMatch = text.match(/(\d+)\s*(?:m|min|mins|minute|minutes)(?![a-zA-Z])/i);
    if (minMatch) {
      minutes = parseInt(minMatch[1], 10);
    } else if (hrMatch) {
      // Fallback: If it found hours but no explicit "m"
      const fallbackRegex = new RegExp(`${hrMatch[1]}\\s*(?:h|hr|hrs|hour|hours)[\\s,and]*(\\d+)`, 'i');
      const fallbackMinMatch = text.match(fallbackRegex);
      if (fallbackMinMatch) {
        minutes = parseInt(fallbackMinMatch[1], 10);
      }
    }

    if (hours === 0 && minutes === 0) {
      return NextResponse.json(
        { error: 'Could not extract screen time. Please provide a clearer screenshot. (Extracted text: ' + text.substring(0, 50) + '...)' },
        { status: 422 }
      );
    }

    // 2. Date Extraction
    let extractedDate = null;
    const dateMatch = text.match(/(today|yesterday|\d{1,2}\s*(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*)/i);
    if (dateMatch) {
      extractedDate = dateMatch[1];
    }

    // 3. Validation
    const totalMinutes = (hours * 60) + minutes;

    if (totalMinutes > 1440) {
      return NextResponse.json(
        { error: 'Invalid time extracted (exceeds 24 hours).' },
        { status: 422 }
      );
    }

    if (userId) {
      // Get Room Goal
      const { data: room } = await supabase
        .from('rooms')
        .select('goal_minutes')
        .eq('id', groupId)
        .single();
      
      const goalMinutes = room?.goal_minutes || 180;
      
      // Determine status
      const status = totalMinutes <= goalMinutes ? 'verified' : 'over_goal';
      
      // Calculate Points
      let points = 20;
      if (totalMinutes <= goalMinutes) {
        points = 100 + (goalMinutes - totalMinutes);
      }
      
      // Upload record
      await supabase.from('daily_logs').insert({
        user_id: userId,
        room_id: groupId,
        screenshot_url: 'placeholder_url', // In a real app we'd upload directly from client to supabase storage
        screen_time_minutes: totalMinutes,
        status: status,
        log_date: clientLogicalDate
      });

      // Streaks and Points logic
      const { data: member } = await supabase
        .from('room_members')
        .select('*')
        .eq('user_id', userId)
        .eq('room_id', groupId)
        .single();

      if (member) {
        const currentStreak = member.current_streak + 1;
        const bestStreak = Math.max(member.best_streak, currentStreak);

        await supabase
          .from('room_members')
          .update({
            total_points: member.total_points + points,
            current_streak: currentStreak,
            best_streak: bestStreak,
          })
          .eq('user_id', userId)
          .eq('room_id', groupId);
      }
    }

    return NextResponse.json({
      success: true,
      data: {
        textExtracted: text,
        extractedDate,
        totalMinutes,
        hours,
        minutes
      }
    });

  } catch (error) {
    console.error('OCR Processing API Error:', error);
    return NextResponse.json(
      { error: error instanceof Error ? error.message : String(error) },
      { status: 500 }
    );
  }
}
