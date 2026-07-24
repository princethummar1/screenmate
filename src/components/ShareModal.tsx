'use client';

import { useState } from 'react';
import { Copy, CheckCircle, Share, X } from 'lucide-react';

interface ShareModalProps {
  inviteCode: string;
  roomName: string;
  onClose: () => void;
}

export default function ShareModal({ inviteCode, roomName, onClose }: ShareModalProps) {
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  const handleCopyCode = () => {
    navigator.clipboard.writeText(inviteCode);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const handleCopyLink = () => {
    const link = `${window.location.origin}/join?code=${inviteCode}`;
    navigator.clipboard.writeText(link);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-[#111] border border-white/10 rounded-3xl p-6 shadow-2xl relative animate-in zoom-in-95 duration-200">
        <button onClick={onClose} className="absolute top-4 right-4 text-gray-400 hover:text-white transition-colors">
          <X className="w-6 h-6" />
        </button>

        <div className="text-center mb-8 mt-2">
          <div className="w-16 h-16 bg-purple-500/20 text-purple-400 rounded-full flex items-center justify-center mx-auto mb-4">
            <Share className="w-8 h-8" />
          </div>
          <h2 className="text-2xl font-bold text-white">Invite Friends</h2>
          <p className="text-gray-400 mt-2">Challenge your friends to join <strong>{roomName}</strong></p>
        </div>

        <div className="space-y-4">
          <div className="p-4 rounded-xl bg-black/50 border border-white/5 flex items-center justify-between">
            <div>
              <p className="text-xs text-gray-500 font-medium uppercase tracking-wider mb-1">Invite Code</p>
              <p className="font-mono text-2xl font-bold text-white tracking-widest">{inviteCode}</p>
            </div>
            <button 
              onClick={handleCopyCode}
              className="p-3 rounded-xl bg-white/10 hover:bg-white/20 transition-colors text-white"
            >
              {copiedCode ? <CheckCircle className="w-5 h-5 text-green-400" /> : <Copy className="w-5 h-5" />}
            </button>
          </div>

          <button 
            onClick={handleCopyLink}
            className="w-full py-4 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold transition-colors flex items-center justify-center gap-2"
          >
            {copiedLink ? <CheckCircle className="w-5 h-5" /> : <Share className="w-5 h-5" />}
            {copiedLink ? 'Link Copied!' : 'Copy Direct Join Link'}
          </button>
        </div>
      </div>
    </div>
  );
}
