import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs, doc, setDoc, serverTimestamp } from "firebase/firestore";
import { Calendar, Check, X, Clock } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export const ContractorCalendar = () => {
  const { user } = useAuth();
  const [workers, setWorkers] = useState<any[]>([]);
  const [attendance, setAttendance] = useState<any[]>([]);
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split("T")[0]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user) loadData();
  }, [user, selectedDate]);

  const loadData = async () => {
    if (!user) return;
    setLoading(true);
    try {
      // 1. Get Workers
      const wQ = query(collection(db, "workers"), where("contractorId", "==", user.uid));
      const wSnap = await getDocs(wQ);
      const wList = wSnap.docs.map(d => ({ id: d.id, ...d.data() }));
      setWorkers(wList);

      // 2. Get Attendance for these workers on selected date
      if (wList.length > 0) {
        const aQ = query(
          collection(db, "attendance"),
          where("contractorId", "==", user.uid),
          where("date", "==", selectedDate)
        );
        const aSnap = await getDocs(aQ);

        const aList: any[] = [];
        aSnap.docs.forEach(d => {
          const data = d.data();
          const workerId = data.user_id.replace("worker_", "");
          aList.push({ id: d.id, workerId, ...data });
        });
        setAttendance(aList);
      }
    } catch (err) {
      console.error("Error loading contractor calendar data:", err);
    }
    setLoading(false);
  };

  const markAttendance = async (workerId: string, status: string, type: string = "full") => {
    if (!user) return;
    const existing = attendance.find(a => a.workerId === workerId && a.status !== 'advance');

    try {
      const docId = existing ? existing.id : `worker_${workerId}_${selectedDate}`;

      const newData = {
        user_id: `worker_${workerId}`,
        contractorId: user.uid,
        date: selectedDate,
        status: status,
        type: type,
        timestamp: serverTimestamp(),
      };

      await setDoc(doc(db, "attendance", docId), newData, { merge: true });

      // Optistic update
      setAttendance(prev => {
        const filtered = prev.filter(a => a.id !== docId);
        return [...filtered, { id: docId, workerId, ...newData }];
      });
    } catch (err) {
      console.error("Error marking worker attendance:", err);
    }
  };

  return (
    <div className="space-y-4 animate-in fade-in">
      <div className="flex items-center gap-3 bg-card p-3 rounded-2xl border border-border">
        <Calendar size={20} className="text-primary" />
        <Input
          type="date"
          value={selectedDate}
          onChange={e => setSelectedDate(e.target.value)}
          className="border-0 focus-visible:ring-0 p-0 h-auto bg-transparent font-bold text-lg"
        />
      </div>

      {loading ? (
        <div className="text-center py-10 text-muted-foreground text-sm">Loading workers...</div>
      ) : workers.length === 0 ? (
        <div className="text-center py-10 text-muted-foreground text-sm">No workers found. Add workers first.</div>
      ) : (
        <div className="space-y-3">
          {workers.map(w => {
            const att = attendance.find(a => a.workerId === w.id && a.status !== 'advance');
            const currentStatus = att?.status;
            const currentType = att?.type;

            return (
              <div key={w.id} className="bg-card p-4 rounded-2xl border border-border">
                <div className="flex justify-between items-center mb-3">
                  <p className="font-bold text-sm">{w.name}</p>
                  <span className="text-[10px] font-bold text-muted-foreground bg-muted px-2 py-0.5 rounded-full">{w.workType || 'Labour'}</span>
                </div>
                <div className="grid grid-cols-3 gap-2">
                  <button
                    onClick={() => markAttendance(w.id, 'present', 'full')}
                    className={`flex flex-col items-center justify-center py-2 rounded-xl border transition-all active:scale-95 ${currentStatus === 'present' && currentType === 'full' ? 'bg-green-500 text-white border-green-500' : 'bg-muted text-foreground border-border'}`}
                  >
                    <Check size={16} className="mb-1" />
                    <span className="text-[10px] font-bold">Present</span>
                  </button>
                  <button
                    onClick={() => markAttendance(w.id, 'present', 'half')}
                    className={`flex flex-col items-center justify-center py-2 rounded-xl border transition-all active:scale-95 ${currentStatus === 'present' && currentType === 'half' ? 'bg-orange-500 text-white border-orange-500' : 'bg-muted text-foreground border-border'}`}
                  >
                    <Clock size={16} className="mb-1" />
                    <span className="text-[10px] font-bold">Half Day</span>
                  </button>
                  <button
                    onClick={() => markAttendance(w.id, 'absent')}
                    className={`flex flex-col items-center justify-center py-2 rounded-xl border transition-all active:scale-95 ${currentStatus === 'absent' ? 'bg-red-500 text-white border-red-500' : 'bg-muted text-foreground border-border'}`}
                  >
                    <X size={16} className="mb-1" />
                    <span className="text-[10px] font-bold">Absent</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
