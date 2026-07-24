'use client';

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine
} from 'recharts';
import { format, parseISO } from 'date-fns';

interface Log {
  id: string;
  log_date: string;
  screen_time_minutes: number;
  status: string;
}

interface HistoryChartProps {
  logs: Log[];
  goalMinutes: number;
}

export default function HistoryChart({ logs, goalMinutes }: HistoryChartProps) {
  if (!logs || logs.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-64 border border-white/5 bg-white/[0.02] rounded-3xl">
        <p className="text-gray-500 font-medium">No history available yet.</p>
        <p className="text-xs text-gray-600 mt-1">Upload your first screenshot to see the chart.</p>
      </div>
    );
  }

  // Format data for Recharts
  const data = logs.map(log => {
    let formattedDate = log.log_date;
    try {
      formattedDate = format(parseISO(log.log_date), 'MMM d');
    } catch(e) {}

    return {
      date: formattedDate,
      minutes: log.screen_time_minutes,
      rawDate: log.log_date,
      status: log.status
    };
  });

  const formatTooltipTime = (value: number) => {
    const h = Math.floor(value / 60);
    const m = value % 60;
    return [`${h}h ${m}m`, 'Screen Time'];
  };

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      const isOver = data.minutes > goalMinutes;
      return (
        <div className="bg-[#0a0a0a] border border-white/10 p-3 rounded-xl shadow-xl">
          <p className="text-gray-400 text-xs mb-1 font-bold uppercase">{label}</p>
          <p className={`text-lg font-black ${isOver ? 'text-red-400' : 'text-green-400'}`}>
            {formatTooltipTime(data.minutes)[0]}
          </p>
          <p className="text-xs text-gray-500 mt-1">
            Goal: {Math.floor(goalMinutes/60)}h {goalMinutes%60}m
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="h-72 w-full mt-4 relative">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 20, right: 10, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="colorMinutes" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#a855f7" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="#a855f7" stopOpacity={0}/>
            </linearGradient>
          </defs>
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
          <Tooltip content={<CustomTooltip />} cursor={{ stroke: '#ffffff20', strokeWidth: 1, strokeDasharray: '5 5' }} />
          
          <ReferenceLine 
            y={goalMinutes} 
            stroke="#ef444450" 
            strokeDasharray="5 5" 
            label={{ position: 'top', value: 'GOAL', fill: '#ef4444', fontSize: 10, fontWeight: 'bold' }} 
          />
          
          <Line 
            type="monotone" 
            dataKey="minutes" 
            stroke="#a855f7" 
            strokeWidth={3}
            dot={{ r: 4, fill: '#0a0a0a', stroke: '#a855f7', strokeWidth: 2 }}
            activeDot={{ r: 6, fill: '#a855f7', stroke: '#fff', strokeWidth: 2 }}
            animationDuration={1500}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
