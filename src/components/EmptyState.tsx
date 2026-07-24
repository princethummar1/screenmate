'use client';

import { useState } from 'react';
import { createRoom, joinRoom } from '@/app/actions';
import { Loader2 } from 'lucide-react';

export default function EmptyState() {
  const [loading, setLoading] = useState(false);
  const [joinCode, setJoinCode] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleCreateRoom = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    
    const formData = new FormData(e.currentTarget);
    const result = await createRoom(formData);
    
    if (result.error) {
      setError(result.error);
      setLoading(false);
    } else {
      window.location.reload();
    }
  };

  const handleJoinRoom = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    
    const result = await joinRoom(joinCode);
    if (result.error) {
      setError(result.error);
      setLoading(false);
    } else {
      window.location.reload();
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-12 animate-in fade-in zoom-in duration-500">
      <div className="text-center space-y-4">
        <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
          Welcome to ScreenMate
        </h1>
        <p className="text-gray-400 text-lg">You aren't in an active challenge room yet. Choose your path below.</p>
      </div>

      {error && (
        <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-center font-medium">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Create Room */}
        <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 shadow-2xl relative overflow-hidden group">
          <div className="absolute inset-0 bg-gradient-to-br from-purple-500/10 to-pink-500/10 opacity-0 group-hover:opacity-100 transition-opacity"></div>
          <h2 className="text-2xl font-bold text-white mb-6 relative z-10">Create a Challenge</h2>
          
          <form onSubmit={handleCreateRoom} className="space-y-6 relative z-10">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Room Name</label>
              <input name="name" type="text" required placeholder="e.g. 30-Day Digital Detox" className="w-full px-4 py-3 rounded-xl bg-black/50 border border-white/10 focus:border-purple-500 focus:outline-none transition-colors" />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Duration</label>
              <div className="grid grid-cols-3 gap-3">
                {[
                  { value: 7, label: '7 Days', sub: 'Sprint' },
                  { value: 14, label: '14 Days', sub: 'Habit' },
                  { value: 30, label: '30 Days', sub: 'Transform' }
                ].map(d => (
                  <label key={d.value} className="cursor-pointer">
                    <input type="radio" name="durationDays" value={d.value} defaultChecked={d.value === 7} className="peer sr-only" />
                    <div className="text-center p-3 rounded-xl border border-white/10 bg-black/50 peer-checked:border-purple-500 peer-checked:bg-purple-500/20 transition-all">
                      <div className="font-bold text-white">{d.label}</div>
                      <div className="text-xs text-gray-400">{d.sub}</div>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Daily Screen Time Goal (Minutes)</label>
              <input name="goalMinutes" type="number" required defaultValue={180} min={1} className="w-full px-4 py-3 rounded-xl bg-black/50 border border-white/10 focus:border-purple-500 focus:outline-none transition-colors" />
              <p className="text-xs text-gray-500 mt-2">180 minutes = 3 hours</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Challenge Reward / Stakes (Optional)</label>
              <input name="reward" type="text" placeholder="e.g. Loser buys dinner, $50 Gift Card" className="w-full px-4 py-3 rounded-xl bg-black/50 border border-white/10 focus:border-purple-500 focus:outline-none transition-colors" />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Daily Reminder Time (Optional)</label>
              <input name="notificationTime" type="time" className="w-full px-4 py-3 rounded-xl bg-black/50 border border-white/10 focus:border-purple-500 focus:outline-none transition-colors" />
              <p className="text-xs text-gray-500 mt-2">Time to remind members to upload their screenshots.</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Midnight Reset Time (Optional)</label>
              <input name="resetTime" type="time" defaultValue="00:00" className="w-full px-4 py-3 rounded-xl bg-black/50 border border-white/10 focus:border-purple-500 focus:outline-none transition-colors" />
              <p className="text-xs text-gray-500 mt-2">When does a new day start? Defaults to 12:00 AM.</p>
            </div>

            <button type="submit" disabled={loading} className="w-full py-3 bg-white text-black rounded-xl font-bold hover:bg-gray-200 transition-colors flex items-center justify-center">
              {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : 'Create Room & Start Challenge'}
            </button>
          </form>
        </div>

        {/* Join Room */}
        <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 shadow-2xl relative overflow-hidden group">
          <div className="absolute inset-0 bg-gradient-to-br from-blue-500/10 to-teal-500/10 opacity-0 group-hover:opacity-100 transition-opacity"></div>
          <h2 className="text-2xl font-bold text-white mb-6 relative z-10">Join a Friend</h2>
          
          <form onSubmit={handleJoinRoom} className="space-y-6 relative z-10 flex flex-col h-[calc(100%-2rem)]">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-300 mb-2">Enter Invite Code</label>
              <input 
                type="text" 
                value={joinCode}
                onChange={e => setJoinCode(e.target.value.toUpperCase())}
                required 
                maxLength={6}
                placeholder="XXXXXX" 
                className="w-full px-4 py-6 text-center text-4xl font-mono tracking-[0.5em] rounded-xl bg-black/50 border border-white/10 focus:border-blue-500 focus:outline-none transition-colors uppercase" 
              />
            </div>

            <button type="submit" disabled={loading || joinCode.length < 6} className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-bold transition-colors disabled:opacity-50 flex items-center justify-center">
              {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : 'Join Room'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
