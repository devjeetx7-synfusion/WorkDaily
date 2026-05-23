import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs, doc, setDoc, deleteDoc, serverTimestamp } from "firebase/firestore";
import { Calendar, Check, X, Clock } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Worker, AttendanceRecord } from "@/types";
import { AttendanceEditDialog, AttendanceEditData } from "@/components/AttendanceEditDialog";

export const ContractorCalendar = () => {
  const { user } = useAuth();
  const [workers, setWorkers] = useState<Worker[]>([]);
  const [attendance, setAttendance] = useState<AttendanceRecord[]>([]);
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split("T")[0]);
  const [loading, setLoading] = useState(true);

  const [showEditDialog, setShowEditDialog] = useState(false);
  const [selectedWorkerId, setSelectedWorkerId] = useState<string | null>(null);
  const [initialDialogData, setInitialDialogData] = useState<Partial<AttendanceEditData>>({});

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
      const wList = wSnap.docs.map(d => ({ id: d.id, ...d.data() } as Worker));
      setWorkers(wList);

      // 2. Get Attendance for these workers on selected date
      if (wList.length > 0) {
        const aQ = query(
          collection(db, "attendance"),
          where("contractorId", "==", user.uid),
          where("date", "==", selectedDate)
        );
        const aSnap = await getDocs(aQ);

        const aList: AttendanceRecord[] = [];
        aSnap.docs.forEach(d => {
          const data = d.data() as AttendanceRecord;
          aList.push({ ...data, id: d.id });
        });
        setAttendance(aList);
      }
    } catch (err) {
      console.error("Error loading contractor calendar data:", err);
    }
    setLoading(false);
  };

  const handleOpenDialog = (workerId: string, status: string, type: string = "full") => {
    const existing = attendance.find(a => a.user_id === `worker_${workerId}` && a.status !== 'advance');
    const advanceRecord = attendance.find(a => a.user_id === `worker_${workerId}` && a.status === 'advance');

    setSelectedWorkerId(workerId);

    if (existing) {
       setInitialDialogData({
         status: existing.status,
         type: existing.type || "full",
         reason: existing.reason || "sick",
         overtime_hours: existing.overtime_hours || 0,
         note: existing.note || "",
         advance_amount: advanceRecord?.advance_amount || 0,
       });
    } else {
       setInitialDialogData({
         status: status,
         type: type,
         reason: "sick",
         overtime_hours: 0,
         note: "",
         advance_amount: advanceRecord?.advance_amount || 0,
       });
    }
    setShowEditDialog(true);
  };

  const saveDay = async (data: AttendanceEditData) => {
    if (!user || !selectedWorkerId) return;

    try {
      if (data.status === "present" || data.status === "absent") {
        const docId = `worker_${selectedWorkerId}_${selectedDate}`;
        const newData = {
          user_id: `worker_${selectedWorkerId}`,
          contractorId: user.uid,
          date: selectedDate,
          status: data.status,
          type: data.status === "present" ? data.type : null,
          reason: data.status === "absent" ? data.reason : null,
          overtime_hours: data.status === "present" ? data.overtime_hours : 0,
          note: data.note || null,
          timestamp: serverTimestamp(),
        };
        await setDoc(doc(db, "attendance", docId), newData, { merge: true });
      }

      const advanceDocId = `worker_${selectedWorkerId}_${selectedDate}_advance`;
      if (data.advance_amount > 0) {
         await setDoc(doc(db, "attendance", advanceDocId), {
           user_id: `worker_${selectedWorkerId}`,
           contractorId: user.uid,
           date: selectedDate,
           status: "advance",
           advance_amount: data.advance_amount,
           note: "Advance Payment",
           timestamp: serverTimestamp(),
         }, { merge: true });
      } else {
         await deleteDoc(doc(db, "attendance", advanceDocId)).catch(() => {});
      }

      setShowEditDialog(false);
      loadData();
    } catch (err) {
      console.error("Error saving worker attendance:", err);
    }
  };

  const deleteDay = async () => {
    if (!user || !selectedWorkerId) return;
    try {
      const docId = `worker_${selectedWorkerId}_${selectedDate}`;
      await deleteDoc(doc(db, "attendance", docId)).catch(() => {});

      const advanceDocId = `worker_${selectedWorkerId}_${selectedDate}_advance`;
      await deleteDoc(doc(db, "attendance", advanceDocId)).catch(() => {});

      setShowEditDialog(false);
      loadData();
    } catch (err) {
      console.error("Error deleting worker attendance:", err);
    }
  };

  const selectedWorkerName = workers.find(w => w.id === selectedWorkerId)?.name || "Worker";

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
          const att = attendance.find(a => a.user_id === `worker_${w.id}` && a.status !== 'advance');
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
                    onClick={() => handleOpenDialog(w.id, 'present', 'full')}
                    className={`flex flex-col items-center justify-center py-2 rounded-xl border transition-all active:scale-95 ${currentStatus === 'present' && currentType === 'full' ? 'bg-green-500 text-white border-green-500' : 'bg-muted text-foreground border-border'}`}
                  >
                    <Check size={16} className="mb-1" />
                    <span className="text-[10px] font-bold">Present</span>
                  </button>
                  <button
                    onClick={() => handleOpenDialog(w.id, 'present', 'half')}
                    className={`flex flex-col items-center justify-center py-2 rounded-xl border transition-all active:scale-95 ${currentStatus === 'present' && currentType === 'half' ? 'bg-orange-500 text-white border-orange-500' : 'bg-muted text-foreground border-border'}`}
                  >
                    <Clock size={16} className="mb-1" />
                    <span className="text-[10px] font-bold">Half Day</span>
                  </button>
                  <button
                    onClick={() => handleOpenDialog(w.id, 'absent')}
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

      <AttendanceEditDialog
        open={showEditDialog}
        onOpenChange={setShowEditDialog}
        title={`${selectedWorkerName} - ${selectedDate}`}
        initialData={initialDialogData}
        onSave={saveDay}
        onDelete={deleteDay}
        showDelete={selectedWorkerId !== null && !!attendance.find(a => a.user_id === `worker_${selectedWorkerId}` && a.status !== 'advance')}
      />

    </div>
  );
};
