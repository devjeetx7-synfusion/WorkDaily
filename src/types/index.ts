export interface Worker {
  id: string;
  name: string;
  phone: string;
  aadhar: string;
  age: number;
  workType: string;
  wage: number;
  contractorId?: string;
  created_at?: any;
}

export interface AttendanceRecord {
  id: string;
  user_id: string;
  contractorId: string;
  date: string;
  status: 'present' | 'absent' | 'advance';
  type?: 'full' | 'half';
  reason?: string;
  overtime_hours?: number;
  note?: string;
  advance_amount?: number;
  timestamp?: any;
}
