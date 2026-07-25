import { createClient } from '@/lib/supabase/server';
import { redirect } from 'next/navigation';
import ActiveRoom from '@/components/ActiveRoom';

export default async function RoomPage({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = await params;
  const roomId = resolvedParams.id;
  const supabase = await createClient();
  
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) {
    redirect('/login');
  }

  // Verify membership
  const { data: membership } = await supabase
    .from('room_members')
    .select('*')
    .eq('user_id', user.id)
    .eq('room_id', roomId)
    .single();

  if (!membership) {
    redirect('/'); // Go back to dashboard if not a member
  }

  // Fetch full room details
  const { data: room } = await supabase
    .from('rooms')
    .select('*')
    .eq('id', roomId)
    .single();

  if (!room) {
    redirect('/');
  }

  // Fetch all members and their profiles for usernames
  const { data: membersData } = await supabase
    .from('room_members')
    .select(`
      user_id,
      total_points,
      current_streak,
      best_streak,
      profiles (
        username
      )
    `)
    .eq('room_id', room.id);

  // Fetch all logs for this room to let the client handle timezone-accurate "logical today"
  const { data: allLogs } = await supabase
    .from('daily_logs')
    .select('*')
    .eq('room_id', room.id)
    .order('log_date', { ascending: true });

  const userLogs = allLogs?.filter(l => l.user_id === user.id) || [];

  const formattedMembers = (membersData || []).map(m => {
    return {
      ...m,
      username: (m.profiles as any)?.username || 'Unknown Player'
    };
  });

  return (
    <main className="min-h-screen bg-[#0a0a0a] text-white py-8">
      <ActiveRoom 
        room={room} 
        members={formattedMembers} 
        currentUserId={user.id} 
        historyLogs={userLogs}
        allRoomLogs={allLogs || []}
      />
    </main>
  );
}
