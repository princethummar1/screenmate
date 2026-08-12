'use client';

import { useState, useEffect } from 'react';
import { UploadCloud, CheckCircle, Flame, Trophy, Loader2, Users, Clock, Bell, ArrowLeft, Activity, Info } from 'lucide-react';
import ShareModal from './ShareModal';
import Link from 'next/link';
import HistoryChart from './HistoryChart';
import { createWorker } from 'tesseract.js';

type Room = {
  id: string;
  name: string;
  invite_code: string;
  goal_minutes: number;
  duration_days: number;
  reward?: string;
  notification_time?: string;
  reset_time?: string;
  start_date: string;
  end_date: string;
};

type Member = {
  user_id: string;
  total_points: number;
  current_streak: number;
  username?: string;
  todayStatus?: 'verified' | 'over_goal' | 'pending';
  uploadTime?: string;
  totalWastedMinutes?: number;
};

interface ActiveRoomProps {
  room: Room;
  members: Member[];
  currentUserId: string;
  todayLog?: {
    screen_time_minutes: number;
    status: string;
    created_at?: string;
  };
  historyLogs?: any[];
  allRoomLogs?: any[];
}

export default function ActiveRoom({ room, members, currentUserId, historyLogs = [], allRoomLogs = [] }: ActiveRoomProps) {
  const [file, setFile] = useState<File | null>(null);

  const getLogicalDateStr = () => {
    const now = new Date();
    const resetTime = room.reset_time || '00:00:00';
    const [h, m] = resetTime.split(':');
    const resetDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), parseInt(h), parseInt(m));
    if (now < resetDate) {
      now.setDate(now.getDate() - 1);
    }
    const y = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    return `${y}-${mm}-${dd}`;
  };

  const clientLogicalToday = getLogicalDateStr();
  const clientTodayLog = historyLogs?.find(l => l.log_date === clientLogicalToday);

  // Compute members with dynamic todayStatus, dynamic points, and total wasted time (only from room start date)
  const dynamicMembers = members.map(m => {
    const userLogs = allRoomLogs?.filter(l => l.user_id === m.user_id) || [];
    const logToday = userLogs.find(l => l.log_date === clientLogicalToday);
    
    const validLogs = userLogs.filter(l => l.log_date >= room.start_date);
    
    const totalWastedMinutes = validLogs.reduce((sum, l) => sum + (l.screen_time_minutes || 0), 0);
    
    const dynamicPoints = validLogs.reduce((sum, l) => {
      if (l.screen_time_minutes <= room.goal_minutes) {
        return sum + 100 + (room.goal_minutes - l.screen_time_minutes);
      }
      return sum + 20; // 20 points for trying
    }, 0);
    
    return {
      ...m,
      total_points: dynamicPoints,
      todayStatus: logToday?.status as any || 'pending',
      uploadTime: logToday?.created_at,
      totalWastedMinutes
    };
  });

  // Format minutes into d h m
  const formatLifetimeMinutes = (total: number = 0) => {
    if (!total) return "0h 0m";
    const d = Math.floor(total / (24 * 60));
    const h = Math.floor((total % (24 * 60)) / 60);
    const m = total % 60;
    
    if (d > 0) return `${d}d ${h}h ${m}m`;
    return `${h}h ${m}m`;
  };

  const [status, setStatus] = useState<'idle' | 'uploading' | 'processing' | 'verified' | 'error'>(
    clientTodayLog ? 'verified' : 'idle'
  );
  const [result, setResult] = useState<{ totalMinutes: number, status: string, textExtracted?: string, extractedDate?: string } | null>(
    clientTodayLog ? { totalMinutes: clientTodayLog.screen_time_minutes, status: clientTodayLog.status } : null
  );
  const [showShareModal, setShowShareModal] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [timeUntilReset, setTimeUntilReset] = useState('');
  const [showOcrDebug, setShowOcrDebug] = useState(false);
  const [ocrProgress, setOcrProgress] = useState<{ status: string, progress: number } | null>(null);

  // Calculate days elapsed
  const start = new Date(room.start_date).getTime();
  const elapsed = Math.max(0, new Date().getTime() - start);
  const daysElapsed = Math.floor(elapsed / (1000 * 60 * 60 * 24)) + 1;
  const daysRemaining = room.duration_days - daysElapsed;
  const progressPercent = Math.min(100, (daysElapsed / room.duration_days) * 100);

  // Countdown timer for Reset Time
  useEffect(() => {
    const updateCountdown = () => {
      const now = new Date();
      const resetTime = room.reset_time || '00:00:00';
      const [h, m] = resetTime.split(':');
      
      let nextReset = new Date(now.getFullYear(), now.getMonth(), now.getDate(), parseInt(h), parseInt(m));
      
      if (now >= nextReset) {
        nextReset.setDate(nextReset.getDate() + 1); // Next day's reset
      }
      
      const diff = nextReset.getTime() - now.getTime();
      const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
      const minutes = Math.floor((diff / 1000 / 60) % 60);
      setTimeUntilReset(`${hours}h ${minutes}m`);
    };
    
    updateCountdown();
    const interval = setInterval(updateCountdown, 60000); // Update every minute
    return () => clearInterval(interval);
  }, [room.reset_time]);

  const handleUpload = async () => {
    if (!file) return;
    setStatus('uploading');
    setOcrProgress({ status: 'Initializing AI Engine...', progress: 0 });
    
    try {
      // 1. Client-Side OCR via Tesseract.js
      const worker = await createWorker('eng', 1, {
        logger: m => {
          if (m.status === 'recognizing text') {
            setOcrProgress({ status: 'Reading Screen Time...', progress: Math.round(m.progress * 100) });
          } else {
            setOcrProgress({ status: 'Loading AI Models...', progress: 10 });
          }
        }
      });
      
      const { data: { text } } = await worker.recognize(file);
      await worker.terminate();
      
      setOcrProgress({ status: 'Verifying with server...', progress: 100 });

      // 2. Send to Server for DB Insertion
      const res = await fetch('/api/ocr', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          text,
          groupId: room.id,
          clientLogicalDate: clientLogicalToday
        }),
      });
      
      const data = await res.json();
      
      if (res.ok) {
        setStatus('verified');
        const isOver = data.data.totalMinutes > room.goal_minutes;
        setResult({ 
          totalMinutes: data.data.totalMinutes,
          status: isOver ? 'over_goal' : 'verified',
          textExtracted: data.data.textExtracted,
          extractedDate: data.data.extractedDate
        });
        // We delay reload slightly so user can see success state, or just reload immediately
        setTimeout(() => window.location.reload(), 1500);
      } else {
        setStatus('error');
        setErrorMsg(data.error);
        if (data.data?.textExtracted) {
           setResult({ totalMinutes: 0, status: 'error', textExtracted: data.data.textExtracted });
        }
      }
    } catch (err) {
      setStatus('error');
      setErrorMsg(String(err));
    } finally {
      setOcrProgress(null);
    }
  };

  const formatTime = (mins: number) => {
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return `${h}h ${m}m`;
  };
  
  const formatUploadTime = (isoString?: string) => {
    if (!isoString) return '';
    return new Date(isoString).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
  };
  
  const formatNotificationTime = (timeString?: string) => {
    if (!timeString) return '';
    const [h, m] = timeString.split(':');
    const date = new Date();
    date.setHours(parseInt(h, 10));
    date.setMinutes(parseInt(m, 10));
    return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
  };

  const isUnderGoal = result ? result.totalMinutes <= room.goal_minutes : false;

  return (
    <div className="max-w-6xl mx-auto p-4 md:p-8 space-y-8 animate-in fade-in zoom-in-95 duration-700">
      
      <Link href="/" className="inline-flex items-center gap-2 text-gray-400 hover:text-white transition-colors text-sm font-bold uppercase tracking-wider">
        <ArrowLeft className="w-4 h-4" /> Back to Dashboard
      </Link>

      {/* Top Navigation & Metrics Bar */}
      <div className="flex flex-col md:flex-row gap-4 items-center justify-between bg-white/5 backdrop-blur-2xl border border-white/10 rounded-3xl p-4 shadow-2xl">
        <div className="flex items-center gap-4 px-4 w-full md:w-auto">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center shadow-lg shadow-purple-500/30 flex-shrink-0">
            <Trophy className="w-6 h-6 text-white" />
          </div>
          <div className="overflow-hidden">
            <h1 className="text-xl font-bold tracking-tight text-white truncate">{room.name}</h1>
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm font-medium text-gray-400 mt-1">
              <span className="flex items-center gap-1"><Clock className="w-4 h-4" /> Resets in {timeUntilReset}</span>
              {room.notification_time && (
                <span className="flex items-center gap-1 text-purple-400"><Bell className="w-4 h-4" /> Reminder at {formatNotificationTime(room.notification_time)}</span>
              )}
            </div>
          </div>
        </div>
        
        <button 
          onClick={() => setShowShareModal(true)}
          className="w-full md:w-auto relative group overflow-hidden rounded-2xl bg-white/10 hover:bg-white/20 transition-all border border-white/10 px-6 py-3"
        >
          <div className="flex items-center justify-center gap-3 relative z-10">
            <span className="text-white font-bold">Invite:</span>
            <span className="px-3 py-1 bg-black/50 rounded-lg text-purple-300 font-mono tracking-widest">{room.invite_code}</span>
            <Users className="w-4 h-4 text-white/50 group-hover:text-white transition-colors" />
          </div>
        </button>
      </div>

      {/* Challenge Countdown Header */}
      <div className="rounded-3xl border border-white/10 bg-[#030303]/80 backdrop-blur-xl p-6 md:p-8 shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-purple-500/10 blur-[100px] rounded-full translate-x-1/3 -translate-y-1/2 pointer-events-none"></div>
        
        <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-6 relative z-10 gap-4">
          <div>
            <h2 className="text-sm font-bold text-gray-400 uppercase tracking-[0.2em]">Challenge Progress</h2>
            <p className="text-4xl font-black text-white mt-2 tracking-tighter">Day {Math.min(daysElapsed, room.duration_days)} <span className="text-gray-600 text-3xl">/ {room.duration_days}</span></p>
          </div>
          <div className="text-left md:text-right w-full md:w-auto">
            <p className="text-lg font-bold bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent">
              {daysRemaining > 0 ? `${daysRemaining} days remaining` : 'Challenge Complete!'}
            </p>
            {room.reward && (
              <p className="text-sm text-yellow-400/90 font-bold mt-2 flex items-center justify-start md:justify-end gap-2 bg-yellow-500/10 px-3 py-1.5 rounded-lg border border-yellow-500/20 w-fit md:ml-auto">
                <Trophy className="w-4 h-4" /> Stakes: {room.reward}
              </p>
            )}
          </div>
        </div>
        <div className="h-4 w-full bg-black/50 rounded-full overflow-hidden border border-white/5 relative z-10 p-0.5">
          <div 
            className="h-full bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 rounded-full shadow-[0_0_15px_rgba(168,85,247,0.5)] transition-all duration-1000 ease-out"
            style={{ width: `${progressPercent}%` }}
          ></div>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
        
        {/* Left Column: Action & Reality Check */}
        <div className="xl:col-span-2 space-y-8 flex flex-col">
          
          {/* Action Card (Dropzone) */}
          {(!result || result.status === 'error') && status !== 'verified' && (
            <div className="rounded-3xl border border-white/10 bg-[#030303]/80 backdrop-blur-xl p-6 md:p-10 shadow-2xl relative overflow-hidden flex-1 flex flex-col justify-center">
              <h3 className="text-sm font-bold text-gray-400 uppercase tracking-[0.2em] mb-6">Today's Evidence</h3>
              
              <div className="relative group cursor-pointer flex-1 flex items-center justify-center min-h-[300px]">
                <div className="absolute -inset-1 bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 rounded-3xl blur-xl opacity-20 group-hover:opacity-40 transition duration-700"></div>
                <div className="relative w-full h-full border-2 border-dashed border-white/10 rounded-3xl p-8 md:p-12 text-center bg-[#0a0a0a]/80 backdrop-blur-sm hover:bg-white/[0.02] hover:border-white/30 transition-all duration-300 flex flex-col items-center justify-center">
                  <input 
                    type="file" 
                    accept="image/*" 
                    onChange={e => e.target.files && setFile(e.target.files[0])} 
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-20"
                  />
                  {!file ? (
                    <div className="space-y-6">
                      <div className="w-20 h-20 mx-auto rounded-full bg-gradient-to-br from-white/5 to-white/10 flex items-center justify-center shadow-inner border border-white/10 group-hover:scale-110 transition-transform duration-500">
                        <UploadCloud className="w-10 h-10 text-purple-400" />
                      </div>
                      <div>
                        <p className="text-white font-bold text-lg md:text-xl">Drop your Screen Time screenshot here</p>
                        <p className="text-gray-500 mt-2 font-medium">PNG, JPG up to 10MB</p>
                      </div>
                    </div>
                  ) : (
                    <div className="space-y-6 relative z-30">
                      <div className="w-20 h-20 mx-auto rounded-full bg-green-500/20 flex items-center justify-center border border-green-500/30">
                        <CheckCircle className="w-10 h-10 text-green-400" />
                      </div>
                      <p className="text-white font-bold text-lg md:text-xl truncate max-w-xs mx-auto">{file.name}</p>
                      
                      {status === 'uploading' && ocrProgress && (
                        <div className="w-full max-w-xs mx-auto mt-4">
                          <div className="flex justify-between text-xs font-bold text-gray-400 mb-2 uppercase tracking-widest">
                            <span>{ocrProgress.status}</span>
                            <span>{ocrProgress.progress}%</span>
                          </div>
                          <div className="h-1.5 w-full bg-black/50 rounded-full overflow-hidden border border-white/5">
                            <div 
                              className="h-full bg-gradient-to-r from-purple-500 to-pink-500 transition-all duration-300"
                              style={{ width: `${ocrProgress.progress}%` }}
                            ></div>
                          </div>
                        </div>
                      )}

                      <button 
                        onClick={(e) => { e.preventDefault(); handleUpload(); }}
                        disabled={status === 'uploading'}
                        className="mt-6 px-10 py-4 bg-white text-black rounded-2xl font-bold shadow-xl shadow-white/10 hover:shadow-white/25 hover:-translate-y-1 transition-all disabled:opacity-50 disabled:hover:translate-y-0 w-full sm:w-auto"
                      >
                        {status === 'uploading' ? <Loader2 className="w-6 h-6 animate-spin mx-auto" /> : 'Process Screenshot via AI'}
                      </button>
                    </div>
                  )}
                </div>
              </div>
              
              {status === 'error' && (
                <div className="mt-6 p-5 rounded-2xl bg-red-500/10 border border-red-500/20 flex flex-col gap-3">
                  <div className="text-red-400 font-medium flex items-center gap-3">
                    <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse flex-shrink-0"></div>
                    <p className="text-sm">{errorMsg}</p>
                  </div>
                  {result?.textExtracted && (
                    <div className="mt-2 text-xs bg-black/50 p-3 rounded-lg text-gray-400 border border-white/5 font-mono max-h-32 overflow-y-auto custom-scrollbar">
                      <span className="text-red-400 font-bold mb-1 block">Raw OCR Extracted Text:</span>
                      {result.textExtracted}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Reality Check Metric Card */}
          {result && status === 'verified' && (
            <div className={`rounded-3xl border p-6 md:p-10 shadow-2xl relative overflow-hidden transition-all duration-700 animate-in slide-in-from-bottom-8 backdrop-blur-xl ${isUnderGoal ? 'border-green-500/20 bg-[#030303]' : 'border-red-500/20 bg-[#030303]'}`}>
              <div className={`absolute top-0 right-0 -mt-20 -mr-20 w-80 h-80 rounded-full blur-[100px] opacity-20 pointer-events-none ${isUnderGoal ? 'bg-green-500' : 'bg-red-500'}`}></div>
              
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-3">
                <h3 className="text-sm font-bold text-gray-400 uppercase tracking-[0.2em]">Today's Reality Check</h3>
                <div className="flex gap-2">
                  {result.extractedDate && (
                    <span className="text-xs font-bold text-purple-400 bg-purple-500/10 px-3 py-1.5 rounded-lg border border-purple-500/20">
                      OCR Date: {result.extractedDate}
                    </span>
                  )}
                  {clientTodayLog?.created_at && (
                    <span className="text-xs font-bold text-gray-500 bg-white/5 px-3 py-1.5 rounded-lg border border-white/5">
                      Uploaded {formatUploadTime(clientTodayLog.created_at)}
                    </span>
                  )}
                </div>
              </div>
              
              <div className="flex items-end gap-4 mb-8">
                <span className={`text-6xl md:text-7xl font-black tracking-tighter drop-shadow-2xl ${isUnderGoal ? 'text-transparent bg-clip-text bg-gradient-to-b from-green-300 to-green-600' : 'text-transparent bg-clip-text bg-gradient-to-b from-red-300 to-red-600'}`}>
                  {formatTime(result.totalMinutes)}
                </span>
                <span className="text-gray-500 text-xl md:text-2xl font-bold mb-3 tracking-tight">logged</span>
              </div>
              
              <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
                {isUnderGoal ? (
                  <div className="inline-flex items-center gap-3 px-6 py-4 bg-green-500/10 text-green-300 rounded-2xl font-bold border border-green-500/20 shadow-[0_0_30px_rgba(34,197,94,0.1)] w-full sm:w-auto">
                    <Flame className="w-5 h-5 text-green-400 flex-shrink-0" />
                    Under goal by {formatTime(room.goal_minutes - result.totalMinutes)}! +{100 + (room.goal_minutes - result.totalMinutes)} pts
                  </div>
                ) : (
                  <div className="inline-flex items-center gap-3 px-6 py-4 bg-red-500/10 text-red-300 rounded-2xl font-bold border border-red-500/20 shadow-[0_0_30px_rgba(239,68,68,0.1)] w-full sm:w-auto">
                    <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse flex-shrink-0"></div>
                    Over goal by {formatTime(result.totalMinutes - room.goal_minutes)}! Streak at risk!
                  </div>
                )}

                {result.textExtracted && (
                  <button onClick={() => setShowOcrDebug(!showOcrDebug)} className="text-xs text-gray-500 hover:text-white transition-colors flex items-center gap-1 font-bold">
                    <Info className="w-4 h-4" /> OCR Debug
                  </button>
                )}
              </div>

              {showOcrDebug && result.textExtracted && (
                 <div className="mt-6 text-xs bg-black/80 p-4 rounded-xl text-gray-400 border border-white/10 font-mono max-h-48 overflow-y-auto custom-scrollbar animate-in slide-in-from-top-4">
                   <span className="text-purple-400 font-bold mb-2 block border-b border-white/10 pb-2">Raw AI Extracted Text:</span>
                   {result.textExtracted}
                 </div>
              )}
            </div>
          )}

          {/* Analytics History Chart Card */}
          <div className="rounded-3xl border border-white/10 bg-[#030303]/80 backdrop-blur-xl p-6 md:p-8 shadow-2xl relative overflow-hidden flex-1">
            <h3 className="text-sm font-bold text-gray-400 uppercase tracking-[0.2em] mb-2 flex items-center gap-2">
              <Activity className="w-4 h-4" /> Analytics Since Room Start
            </h3>
            <p className="text-xs text-gray-500 mb-6 font-medium">Historical screen time of all members mapped against the room goal.</p>
            <HistoryChart allRoomLogs={allRoomLogs} members={dynamicMembers} goalMinutes={room.goal_minutes} roomStartDate={room.start_date} />
          </div>

        </div>

        {/* Right Column: Leaderboard */}
        <div className="xl:col-span-1 rounded-3xl border border-white/10 bg-[#030303]/80 backdrop-blur-xl p-5 md:p-8 shadow-2xl flex flex-col min-h-[400px] xl:min-h-0 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-32 bg-gradient-to-b from-white/5 to-transparent pointer-events-none"></div>
          
          <h2 className="text-lg font-bold text-white mb-8 flex items-center gap-3 relative z-10">
            <Trophy className="w-5 h-5 text-yellow-400 drop-shadow-[0_0_10px_rgba(250,204,21,0.5)]" />
            Live Leaderboard
          </h2>

          <div className="space-y-4 flex-1 relative z-10 overflow-y-auto pr-2 custom-scrollbar">
            {dynamicMembers.length === 0 && (
              <p className="text-gray-500 text-sm italic">No members found.</p>
            )}
            {dynamicMembers.sort((a, b) => b.total_points - a.total_points).map((member, index) => {
              const isMe = member.user_id === currentUserId;
              
              let rankBadge = <div className="w-8 h-8 rounded-xl bg-white/5 flex items-center justify-center font-bold text-xs text-gray-500 border border-white/10">{index + 1}</div>;
              if (index === 0) rankBadge = <div className="w-8 h-8 rounded-xl bg-yellow-500/10 border border-yellow-500/20 flex items-center justify-center text-lg drop-shadow-lg">🥇</div>;
              if (index === 1) rankBadge = <div className="w-8 h-8 rounded-xl bg-gray-300/10 border border-gray-400/20 flex items-center justify-center text-lg drop-shadow-lg">🥈</div>;
              if (index === 2) rankBadge = <div className="w-8 h-8 rounded-xl bg-orange-500/10 border border-orange-500/20 flex items-center justify-center text-lg drop-shadow-lg">🥉</div>;

              return (
                <div key={member.user_id} className={`p-4 rounded-2xl border transition-all duration-300 flex items-center justify-between group ${isMe ? 'border-indigo-500/30 bg-indigo-500/10 shadow-[0_0_20px_rgba(99,102,241,0.1)]' : 'border-white/5 bg-black/40 hover:bg-white/5'}`}>
                  <div className="flex items-center gap-4 min-w-0 flex-1">
                    {rankBadge}
                    <div className="flex-1 min-w-0">
                      <p className={`font-bold text-sm truncate flex items-center gap-2 ${isMe ? 'text-white' : 'text-gray-300 group-hover:text-white transition-colors'}`}>
                        {member.username} {isMe && <span className="text-[10px] bg-purple-500/20 text-purple-400 px-2 py-0.5 rounded-full uppercase tracking-wider">You</span>}
                      </p>
                      
                      <div className="flex items-center gap-3 mt-1.5 flex-wrap">
                        <span className="text-xs text-orange-400 flex items-center gap-1 font-bold bg-orange-500/10 px-2 py-0.5 rounded-md border border-orange-500/20">
                          <Flame className="w-3 h-3" /> {member.current_streak}
                        </span>
                        
                        <span className="text-xs text-gray-400 flex items-center gap-1 font-bold bg-white/5 px-2 py-0.5 rounded-md border border-white/5" title="Total Time Since Room Start">
                          <Clock className="w-3 h-3 text-gray-500" /> {formatLifetimeMinutes(member.totalWastedMinutes)}
                        </span>

                        {member.todayStatus === 'verified' && (
                          <div className="flex items-center gap-1.5 text-[10px] font-bold text-green-400 uppercase tracking-wider">
                            <span className="w-2.5 h-2.5 rounded-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]"></span>
                            {member.uploadTime ? formatUploadTime(member.uploadTime) : 'Verified'}
                          </div>
                        )}
                        {member.todayStatus === 'over_goal' && (
                          <div className="flex items-center gap-1.5 text-[10px] font-bold text-red-400 uppercase tracking-wider">
                            <span className="w-2.5 h-2.5 rounded-full bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.6)]"></span>
                            {member.uploadTime ? formatUploadTime(member.uploadTime) : 'Over Goal'}
                          </div>
                        )}
                        {(!member.todayStatus || member.todayStatus === 'pending') && (
                          <div className="flex items-center gap-1.5 text-[10px] font-bold text-gray-500 uppercase tracking-wider">
                            <span className="w-2.5 h-2.5 rounded-full bg-gray-700 border border-gray-600"></span>
                            Pending
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                  
                  <div className="text-right flex-shrink-0 ml-4">
                    <p className={`font-black text-lg tracking-tight ${isMe ? 'text-indigo-400' : 'text-white'}`}>{member.total_points}</p>
                    <p className="text-[9px] text-gray-500 font-bold uppercase tracking-widest mt-0.5">pts</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {showShareModal && (
        <ShareModal 
          inviteCode={room.invite_code} 
          roomName={room.name} 
          onClose={() => setShowShareModal(false)} 
        />
      )}
    </div>
  );
}
