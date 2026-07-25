'use client';

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
  Legend
} from 'recharts';
import { format, parseISO } from 'date-fns';

interface Log {
  id: string;
  user_id: string;
  log_date: string;
  screen_time_minutes: number;
  status: string;
}

interface Member {
  user_id: string;
  username?: string;
}

interface HistoryChartProps {
  allRoomLogs: Log[];
  members: Member[];
  goalMinutes: number;
}

// A vibrant color palette for different players
const CHART_COLORS = ['#a855f7', '#3b82f6', '#ec4899', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6'];

export default function HistoryChart({ allRoomLogs = [], members = [], goalMinutes }: HistoryChartProps) {
  if (!allRoomLogs || allRoomLogs.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-64 border border-white/5 bg-white/[0.02] rounded-3xl">
        <p className="text-gray-500 font-medium">No history available yet.</p>
        <p className="text-xs text-gray-600 mt-1">Upload your first screenshot to start the graph.</p>
      </div>
    );
  }

  // 1. Extract unique dates and sort chronologically
  const uniqueDates = Array.from(new Set(allRoomLogs.map(l => l.log_date))).sort();

  // 2. Build flattened data array for Recharts
  const data = uniqueDates.map(dateStr => {
    let formattedDate = dateStr;
    try {
      formattedDate = format(parseISO(dateStr), 'MMM d');
    } catch(e) {}

    const dayObj: any = {
      date: formattedDate,
      rawDate: dateStr
    };

    // For this specific date, attach each member's screen time
    members.forEach(member => {
      const memberLog = allRoomLogs.find(l => l.user_id === member.user_id && l.log_date === dateStr);
      if (memberLog) {
        dayObj[member.user_id] = memberLog.screen_time_minutes;
      }
    });

    return dayObj;
  });

  const formatTooltipTime = (value: number) => {
    const h = Math.floor(value / 60);
    const m = value % 60;
    return `${h}h ${m}m`;
  };

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-[#0a0a0a]/95 backdrop-blur-md border border-white/10 p-4 rounded-2xl shadow-2xl min-w-[200px]">
          <p className="text-gray-400 text-xs mb-3 font-bold uppercase tracking-widest border-b border-white/10 pb-2">{label}</p>
          <div className="space-y-2">
            {payload.map((entry: any, index: number) => {
              const memberName = members.find(m => m.user_id === entry.dataKey)?.username || 'Unknown';
              const isOver = entry.value > goalMinutes;
              return (
                <div key={index} className="flex items-center justify-between gap-4">
                  <div className="flex items-center gap-2">
                    <span className="w-3 h-3 rounded-full" style={{ backgroundColor: entry.color }}></span>
                    <span className="text-white text-sm font-bold truncate max-w-[100px]">{memberName}</span>
                  </div>
                  <span className={`text-sm font-black ${isOver ? 'text-red-400' : 'text-green-400'}`}>
                    {formatTooltipTime(entry.value)}
                  </span>
                </div>
              );
            })}
          </div>
          <div className="mt-3 pt-2 border-t border-white/10 flex justify-between items-center text-xs text-gray-500 font-bold">
            <span>Room Goal:</span>
            <span>{Math.floor(goalMinutes/60)}h {goalMinutes%60}m</span>
          </div>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="h-80 w-full mt-4 relative">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 20, right: 20, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#ffffff10" vertical={false} />
          <XAxis 
            dataKey="date" 
            stroke="#ffffff40" 
            fontSize={12} 
            tickMargin={10}
            axisLine={false}
            tickLine={false}
          />
          <YAxis 
            stroke="#ffffff40" 
            fontSize={12} 
            axisLine={false}
            tickLine={false}
            tickFormatter={(value) => `${Math.floor(value/60)}h`}
          />
          <Tooltip 
            content={<CustomTooltip />} 
            cursor={{ stroke: '#ffffff20', strokeWidth: 1, strokeDasharray: '5 5' }} 
          />
          
          <Legend 
            wrapperStyle={{ paddingTop: '20px', fontSize: '12px' }}
            formatter={(value) => {
               const name = members.find(m => m.user_id === value)?.username || value;
               return <span className="text-gray-300 font-bold">{name}</span>;
            }}
          />

          <ReferenceLine 
            y={goalMinutes} 
            stroke="#ef444450" 
            strokeDasharray="5 5" 
            label={{ position: 'insideTopLeft', value: 'GOAL', fill: '#ef4444', fontSize: 10, fontWeight: 'bold' }} 
          />
          
          {members.map((member, index) => {
            const color = CHART_COLORS[index % CHART_COLORS.length];
            return (
              <Line 
                key={member.user_id}
                type="monotone" 
                dataKey={member.user_id} 
                name={member.user_id}
                stroke={color} 
                strokeWidth={3}
                connectNulls={true}
                dot={{ r: 4, fill: '#0a0a0a', stroke: color, strokeWidth: 2 }}
                activeDot={{ r: 6, fill: color, stroke: '#fff', strokeWidth: 2 }}
                animationDuration={1500}
              />
            );
          })}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
