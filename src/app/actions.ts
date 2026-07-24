'use server'

import { createClient } from '@/lib/supabase/server'
import { revalidatePath } from 'next/cache'

function generateInviteCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let result = ''
  for (let i = 0; i < 6; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

export async function createRoom(formData: FormData) {
  const supabase = await createClient()
  
  const { data: { user }, error: authError } = await supabase.auth.getUser()
  if (authError || !user) {
    return { error: 'Unauthorized' }
  }

  const name = formData.get('name') as string
  const durationDays = parseInt(formData.get('durationDays') as string, 10)
  const goalMinutes = parseInt(formData.get('goalMinutes') as string, 10)
  const reward = formData.get('reward') as string || null
  const notificationTime = formData.get('notificationTime') as string || null
  const resetTime = formData.get('resetTime') as string || '00:00:00'

  if (!name || !durationDays || !goalMinutes) {
    return { error: 'Missing fields' }
  }

  // Check max 3 rooms
  const { count } = await supabase
    .from('room_members')
    .select('*', { count: 'exact', head: true })
    .eq('user_id', user.id)

  if (count !== null && count >= 3) {
    return { error: 'You can only be a member of up to 3 rooms at a time.' }
  }

  const inviteCode = generateInviteCode()
  
  // Calculate end date
  const startDate = new Date()
  const endDate = new Date(startDate)
  endDate.setDate(startDate.getDate() + durationDays)

  // 1. Create Room
  const { data: room, error: roomError } = await supabase
    .from('rooms')
    .insert({
      name,
      invite_code: inviteCode,
      goal_minutes: goalMinutes,
      duration_days: durationDays,
      reward: reward,
      notification_time: notificationTime,
      reset_time: resetTime,
      start_date: startDate.toISOString().split('T')[0],
      end_date: endDate.toISOString().split('T')[0],
      owner_id: user.id,
      is_active: true
    })
    .select()
    .single()

  if (roomError) {
    return { error: roomError.message }
  }

  // 2. Add creator to room_members
  const { error: memberError } = await supabase
    .from('room_members')
    .insert({
      room_id: room.id,
      user_id: user.id,
      total_points: 0,
      current_streak: 0,
      best_streak: 0
    })

  if (memberError) {
    return { error: memberError.message }
  }

  revalidatePath('/')
  return { success: true, room }
}

export async function joinRoom(inviteCode: string) {
  const supabase = await createClient()
  
  const { data: { user }, error: authError } = await supabase.auth.getUser()
  if (authError || !user) {
    return { error: 'Unauthorized' }
  }

  if (!inviteCode) return { error: 'Invite code required' }

  // Check max 3 rooms
  const { count } = await supabase
    .from('room_members')
    .select('*', { count: 'exact', head: true })
    .eq('user_id', user.id)

  if (count !== null && count >= 3) {
    return { error: 'You can only be a member of up to 3 rooms at a time.' }
  }

  // 1. Find room by code
  const { data: room, error: roomError } = await supabase
    .from('rooms')
    .select('id')
    .eq('invite_code', inviteCode.toUpperCase())
    .single()

  if (roomError || !room) {
    return { error: 'Invalid invite code or room does not exist.' }
  }

  // 2. Check if already joined
  const { data: existingMember } = await supabase
    .from('room_members')
    .select('*')
    .eq('room_id', room.id)
    .eq('user_id', user.id)
    .single()

  if (existingMember) {
    revalidatePath('/')
    return { success: true, message: 'Already a member' }
  }

  // 3. Join room
  const { error: joinError } = await supabase
    .from('room_members')
    .insert({
      room_id: room.id,
      user_id: user.id,
      total_points: 0,
      current_streak: 0,
      best_streak: 0
    })

  if (joinError) {
    return { error: joinError.message }
  }

  revalidatePath('/')
  return { success: true }
}
