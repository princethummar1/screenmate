import { createClient } from '@/lib/supabase/server';
import { redirect } from 'next/navigation';

export default async function JoinPage({ searchParams }: { searchParams: { code?: string } }) {
  const code = searchParams.code;
  
  if (!code) {
    redirect('/');
  }

  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  
  if (!user) {
    // Redirect to login but persist the join intent
    redirect(`/login?next=/join?code=${code}`);
  }

  // 1. Find room
  const { data: room } = await supabase
    .from('rooms')
    .select('id')
    .eq('invite_code', code.toUpperCase())
    .single();

  if (!room) {
    return (
      <div className="min-h-screen bg-[#0a0a0a] text-white flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-red-400">Invalid Invite Code</h1>
          <p className="text-gray-400 mt-2">The room you are trying to join does not exist.</p>
          <a href="/" className="mt-6 inline-block text-purple-400 hover:text-purple-300 underline">Go Home</a>
        </div>
      </div>
    );
  }

  // 2. Check membership
  const { data: existing } = await supabase
    .from('room_members')
    .select('*')
    .eq('room_id', room.id)
    .eq('user_id', user.id)
    .single();

  if (!existing) {
    await supabase.from('room_members').insert({
      room_id: room.id,
      user_id: user.id,
      total_points: 0,
      current_streak: 0,
      best_streak: 0
    });
  }

  // Success, redirect to dashboard
  redirect('/');
}
