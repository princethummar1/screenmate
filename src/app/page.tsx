import { createClient } from '@/lib/supabase/server';
import { redirect } from 'next/navigation';
import EmptyState from '@/components/EmptyState';
import Link from 'next/link';
import { Users, Clock, ArrowRight, Trophy } from 'lucide-react';

export default async function Dashboard() {
  const supabase = await createClient();
  
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) {
    redirect('/login');
  }

  // Fetch all room memberships for the user
  const { data: memberships } = await supabase
    .from('room_members')
    .select(`
      room_id,
      rooms (
        id,
        name,
        goal_minutes,
        duration_days,
        start_date,
        is_active,
        reward
      )
    `)
    .eq('user_id', user.id);

  if (!memberships || memberships.length === 0) {
    return (
      <main className="min-h-screen bg-[#0a0a0a] text-white py-12">
        <EmptyState />
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#0a0a0a] text-white p-4 md:p-12 animate-in fade-in duration-500">
      <div className="max-w-6xl mx-auto space-y-12">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-4xl font-black tracking-tight bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
              Your Challenges
            </h1>
            <p className="text-gray-400 font-medium mt-2">Manage your active digital detox rooms.</p>
          </div>
          {memberships.length < 3 && (
            <a 
              href="#new-challenge"
              className="px-6 py-3 bg-white/10 hover:bg-white/20 text-white rounded-xl font-bold transition-all border border-white/10 hidden md:block"
            >
              + New Challenge
            </a>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {memberships.map((m: any) => {
            const room = m.rooms;
            const start = new Date(room.start_date).getTime();
            const elapsed = Math.max(0, new Date().getTime() - start);
            const daysElapsed = Math.floor(elapsed / (1000 * 60 * 60 * 24)) + 1;
            const progressPercent = Math.min(100, (daysElapsed / room.duration_days) * 100);

            return (
              <Link 
                href={`/room/${room.id}`} 
                key={room.id}
                className="group relative rounded-3xl border border-white/10 bg-[#030303]/80 backdrop-blur-xl p-8 shadow-2xl overflow-hidden hover:border-purple-500/50 transition-all duration-300 hover:-translate-y-2"
              >
                <div className="absolute inset-0 bg-gradient-to-br from-purple-500/5 to-pink-500/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>
                
                <div className="relative z-10 flex flex-col h-full">
                  <div className="flex justify-between items-start mb-6">
                    <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center shadow-lg">
                      <Trophy className="w-6 h-6 text-white" />
                    </div>
                    {room.reward && (
                      <span className="text-[10px] font-bold uppercase tracking-widest bg-yellow-500/20 text-yellow-400 px-3 py-1 rounded-full border border-yellow-500/20">
                        Prize Room
                      </span>
                    )}
                  </div>
                  
                  <h2 className="text-2xl font-bold text-white mb-2">{room.name}</h2>
                  <p className="text-gray-400 font-medium mb-8 flex items-center gap-2">
                    <Clock className="w-4 h-4" /> Goal: {room.goal_minutes} mins/day
                  </p>

                  <div className="mt-auto space-y-3">
                    <div className="flex justify-between text-xs font-bold text-gray-500 uppercase tracking-widest">
                      <span>Day {Math.min(daysElapsed, room.duration_days)}</span>
                      <span>{room.duration_days} Days</span>
                    </div>
                    <div className="h-2 w-full bg-black/50 rounded-full overflow-hidden border border-white/5 p-0.5">
                      <div 
                        className="h-full bg-gradient-to-r from-purple-500 to-pink-500 rounded-full shadow-[0_0_10px_rgba(168,85,247,0.5)]"
                        style={{ width: `${progressPercent}%` }}
                      ></div>
                    </div>
                  </div>
                  
                  <div className="mt-6 flex items-center justify-between text-sm font-bold text-purple-400 group-hover:text-pink-400 transition-colors">
                    Enter Room <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
        
        {memberships.length < 3 && (
          <div id="new-challenge" className="pt-12 border-t border-white/10">
            <h2 className="text-2xl font-bold text-white mb-8 text-center">Join or Create another Room</h2>
            <EmptyState />
          </div>
        )}
      </div>
    </main>
  );
}
