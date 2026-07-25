import { Loader2 } from 'lucide-react';

export default function Loading() {
  return (
    <div className="min-h-screen bg-[#0a0a0a] text-white flex flex-col items-center justify-center p-4">
      <div className="relative flex items-center justify-center">
        {/* Glowing background blob */}
        <div className="absolute w-32 h-32 bg-purple-600 rounded-full blur-[50px] opacity-20 animate-pulse"></div>
        
        {/* Spinner */}
        <Loader2 className="w-12 h-12 text-purple-400 animate-spin relative z-10" />
      </div>
      <p className="text-gray-400 mt-6 font-medium animate-pulse tracking-widest uppercase text-sm">
        Loading Challenge Data...
      </p>
    </div>
  );
}
