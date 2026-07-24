'use client';

import { useState } from 'react';
import { UploadCloud, Copy, CheckCircle, Flame, Trophy, Loader2 } from 'lucide-react';

export default function Dashboard() {
  const [file, setFile] = useState<File | null>(null);
  const [status, setStatus] = useState<'idle' | 'uploading' | 'processing' | 'verified' | 'error'>('idle');
  const [copied, setCopied] = useState(false);
  const [result, setResult] = useState<{ totalMinutes: number } | null>(null);

  const handleCopy = () => {
    navigator.clipboard.writeText('X7Y9Z1');
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!file) return;
    setStatus('uploading');
    
    // Simulate steps for UI demonstration
    setTimeout(() => setStatus('processing'), 1500);
    
    const formData = new FormData();
    formData.append('file', file);
    formData.append('groupId', 'demo-group-id');

    try {
      const res = await fetch('/api/ocr', {
        method: 'POST',
        body: formData,
      });
      const data = await res.json();
      
      if (res.ok) {
        setStatus('verified');
        setResult({ totalMinutes: data.data.totalMinutes });
      } else {
        setStatus('error');
        console.error(data.error);
      }
    } catch (err) {
      setStatus('error');
    }
  };

  return (
    <div className="min-h-screen bg-[#0a0a0a] text-white font-sans selection:bg-purple-500/30">
      <div className="max-w-4xl mx-auto p-6 space-y-8">
        
        {/* Header / Group Info - Glassmorphism */}
        <header className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 shadow-2xl">
          <div className="absolute top-0 right-0 -mt-16 -mr-16 w-64 h-64 bg-purple-600 rounded-full blur-3xl opacity-20 pointer-events-none"></div>
          
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 relative z-10">
            <div>
              <h1 className="text-3xl font-bold bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
                Digital Detox Squad
              </h1>
              <p className="text-gray-400 mt-2 flex items-center gap-2">
                Daily Goal: <span className="px-3 py-1 bg-green-500/20 text-green-400 rounded-full text-sm font-semibold border border-green-500/30">3 hrs (180m)</span>
              </p>
            </div>

            <button 
              onClick={handleCopy}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 transition-all border border-white/5 shadow-sm group"
            >
              <span className="font-mono text-gray-200">X7Y9Z1</span>
              {copied ? <CheckCircle className="w-4 h-4 text-green-400" /> : <Copy className="w-4 h-4 text-gray-400 group-hover:text-white transition-colors" />}
            </button>
          </div>
        </header>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Main Action Area */}
          <div className="lg:col-span-2 space-y-6">
            <div className="rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 shadow-2xl relative overflow-hidden transition-all hover:border-purple-500/30">
              <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
                <UploadCloud className="w-5 h-5 text-purple-400" />
                Log Today's Screen Time
              </h2>

              <div className="relative group cursor-pointer">
                <div className={`absolute -inset-1 bg-gradient-to-r from-purple-600 to-pink-600 rounded-2xl blur opacity-25 group-hover:opacity-50 transition duration-1000 group-hover:duration-200 ${file ? 'opacity-50' : ''}`}></div>
                <div className="relative border-2 border-dashed border-white/20 rounded-xl p-10 text-center bg-[#0a0a0a] hover:bg-white/5 transition-colors">
                  <input 
                    type="file" 
                    accept="image/*" 
                    onChange={handleFileChange} 
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                  />
                  {!file ? (
                    <div className="space-y-4">
                      <div className="w-16 h-16 mx-auto rounded-full bg-purple-500/20 flex items-center justify-center">
                        <UploadCloud className="w-8 h-8 text-purple-400" />
                      </div>
                      <p className="text-gray-300 font-medium text-lg">Drag & drop your screenshot here</p>
                      <p className="text-gray-500 text-sm">or click to browse files</p>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      <div className="w-16 h-16 mx-auto rounded-full bg-green-500/20 flex items-center justify-center">
                        <CheckCircle className="w-8 h-8 text-green-400" />
                      </div>
                      <p className="text-white font-medium text-lg">{file.name}</p>
                      <button 
                        onClick={(e) => { e.preventDefault(); handleUpload(); }}
                        disabled={status !== 'idle' && status !== 'error'}
                        className="mt-4 px-6 py-2 bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 text-white rounded-lg font-medium shadow-lg transition-all z-10 relative disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Verify Screen Time
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* Status Indicator */}
              {status !== 'idle' && (
                <div className="mt-6 p-4 rounded-xl border border-white/5 bg-white/5 flex items-center gap-4">
                  {status === 'uploading' && <><Loader2 className="w-5 h-5 text-blue-400 animate-spin" /> <span className="text-blue-400">Uploading & Cleaning image...</span></>}
                  {status === 'processing' && <><Loader2 className="w-5 h-5 text-purple-400 animate-spin" /> <span className="text-purple-400">Reading text via OCR...</span></>}
                  {status === 'verified' && <><CheckCircle className="w-5 h-5 text-green-400" /> <span className="text-green-400">Verified! Logged {result?.totalMinutes} minutes.</span></>}
                  {status === 'error' && <><span className="w-2 h-2 rounded-full bg-red-500" /> <span className="text-red-400">Verification failed. Please try a clearer screenshot.</span></>}
                </div>
              )}
            </div>
          </div>

          {/* Leaderboard */}
          <div className="lg:col-span-1 rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-2xl relative">
            <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
              <Trophy className="w-5 h-5 text-yellow-400" />
              Leaderboard
            </h2>

            <div className="space-y-4">
              {[
                { rank: 1, name: 'Alice', points: 1450, time: '2h 15m', streak: 12 },
                { rank: 2, name: 'You', points: 1320, time: '2h 45m', streak: 5 },
                { rank: 3, name: 'Charlie', points: 980, time: '4h 10m', streak: 2 },
              ].map((user) => (
                <div key={user.name} className="p-4 rounded-xl border border-white/5 bg-white/5 flex items-center justify-between hover:bg-white/10 transition-colors">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-gray-700 flex items-center justify-center font-bold text-sm text-gray-300">
                      {user.rank}
                    </div>
                    <div>
                      <p className="font-medium text-gray-200">{user.name}</p>
                      <p className="text-xs text-gray-500">{user.time} today</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-purple-400">{user.points} pts</p>
                    <p className="text-xs text-orange-400 flex items-center justify-end gap-1 font-medium">
                      <Flame className="w-3 h-3" /> {user.streak}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
