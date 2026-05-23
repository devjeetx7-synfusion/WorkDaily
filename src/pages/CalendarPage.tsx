import { useState, useEffect } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useLanguage } from "@/contexts/LanguageContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs, doc, setDoc, deleteDoc, serverTimestamp } from "firebase/firestore";

import { ContractorCalendar } from "@/pages/ContractorCalendar";
import { AttendanceEditDialog, AttendanceEditData } from "@/components/AttendanceEditDialog";

interface DayData {
  status: string;
  type?: string;
  reason?: string;
  overtime_hours?: number;
  note?: string;
  advance_amount?: number;
}

const CalendarPage = () => {
  const { user, userData } = useAuth();
  const { t } = useLanguage();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [dayMap, setDayMap] = useState<Record<number, DayData>>({});
  const [selectedDay, setSelectedDay] = useState<number | null>(null);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [initialDialogData, setInitialDialogData] = useState<Partial<AttendanceEditData>>({});

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayOfWeek = new Date(year, month, 1).getDay();

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];
  const dayLabels = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];
  const absenceReasons = ["sick", "personal", "holiday", "weather", "other"];

  const currentRole = userData?.role || localStorage.getItem("dailywork_role");

  useEffect(() => {
    if (!user || currentRole === "contractor") return;
    loadMonth();
  }, [user, month, year, currentRole]);

  const loadMonth = async () => {
    if (!user) return;
    const startDate = `${year}-${String(month + 1).padStart(2, "0")}-01`;
    const endDate = `${year}-${String(month + 1).padStart(2, "0")}-31`;

    try {
      // Simple query: only filter by user_id
      const q = query(
        collection(db, "attendance"),
        where("user_id", "==", user.uid)
      );

      const snap = await getDocs(q);
      const map: Record<number, DayData> = {};
      snap.docs.forEach((d) => {
        const data = d.data();
        // Client-side date filtering
        if (data.date < startDate || data.date > endDate) return;
        const day = parseInt(data.date.split("-")[2], 10);

        if (data.status === "advance") {
          if (map[day]) {
            map[day].advance_amount = (map[day].advance_amount || 0) + data.advance_amount;
          } else {
            map[day] = {
              status: "advance",
              advance_amount: data.advance_amount
            };
          }
          return;
        }

        if (map[day] && map[day].status === "advance") {
             map[day] = {
               status: data.status,
               type: data.type,
               reason: data.reason,
               overtime_hours: data.overtime_hours,
               note: data.note,
               advance_amount: map[day].advance_amount
             };
             return;
        }

        map[day] = {
          status: data.status,
          type: data.type,
          reason: data.reason,
          overtime_hours: data.overtime_hours,
          note: data.note,
          advance_amount: data.advance_amount
        };
      });
      setDayMap(map);
    } catch (err) {
      console.error("Error loading calendar:", err);
    }
  };

  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1));

  const todayDate = new Date();
  const isCurrentMonth = todayDate.getFullYear() === year && todayDate.getMonth() === month;

  const handleDayClick = (day: number) => {
    const data = dayMap[day];
    setSelectedDay(day);
    if (data) {
      setInitialDialogData({
        status: data.status,
        type: data.type,
        reason: data.reason,
        overtime_hours: data.overtime_hours,
        note: data.note,
        advance_amount: data.advance_amount,
      });
    } else {
      setInitialDialogData({});
    }
    setShowEditDialog(true);
  };

  const saveDay = async (data: AttendanceEditData) => {
    if (!user || selectedDay === null) return;
    try {
      const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(selectedDay).padStart(2, "0")}`;

      if (data.status === "present" || data.status === "absent") {
        const docId = `${user.uid}_${dateStr}`;
        await setDoc(doc(db, "attendance", docId), {
          user_id: user.uid,
          date: dateStr,
          status: data.status,
          type: data.status === "present" ? data.type : null,
          reason: data.status === "absent" ? data.reason : null,
          overtime_hours: data.status === "present" ? data.overtime_hours : 0,
          note: data.note || null,
          timestamp: serverTimestamp(),
        });
      }

      if (data.advance_amount > 0) {
         const advanceDocId = `${user.uid}_${dateStr}_advance`;
         await setDoc(doc(db, "attendance", advanceDocId), {
           user_id: user.uid,
           date: dateStr,
           status: "advance",
           advance_amount: data.advance_amount,
           note: "Advance Payment (Calendar)",
           timestamp: serverTimestamp(),
         });
      } else {
         const advanceDocId = `${user.uid}_${dateStr}_advance`;
         await deleteDoc(doc(db, "attendance", advanceDocId)).catch(() => {});
      }

      setShowEditDialog(false);
      loadMonth();
    } catch (err) {
      console.error("Error saving day:", err);
    }
  };

  const deleteDay = async () => {
    if (!user || selectedDay === null) return;
    try {
      const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(selectedDay).padStart(2, "0")}`;
      const docId = `${user.uid}_${dateStr}`;
      await deleteDoc(doc(db, "attendance", docId)).catch(() => {});

      const advanceDocId = `${user.uid}_${dateStr}_advance`;
      await deleteDoc(doc(db, "attendance", advanceDocId)).catch(() => {});

      setShowEditDialog(false);
      loadMonth();
    } catch (err) {
      console.error("Error deleting day:", err);
    }
  };

  if (currentRole === "contractor") {
    return (
      <div className="min-h-screen bg-background pb-20 md:pb-6 pt-6 px-4 max-w-3xl mx-auto">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-foreground">Mark Attendance</h1>
          <p className="text-sm text-muted-foreground mt-1">Daily attendance for all workers</p>
        </div>
        <ContractorCalendar />

      </div>
    );
  }

  const presentCount = Object.values(dayMap).filter((d) => d.status === "present").length;
  const absentCount = Object.values(dayMap).filter((d) => d.status === "absent").length;

  return (
    <div className="min-h-screen bg-background pb-20 md:pb-6">
      <div className="mx-auto max-w-lg px-4 pt-6">
        <h1 className="text-xl font-bold text-foreground mb-4">{t("calendar")}</h1>

        {/* Month Selector */}
        <div className="flex items-center justify-between mb-4">
          <button onClick={prevMonth} className="touch-target p-2 rounded-lg active:bg-card">
            <ChevronLeft size={24} />
          </button>
          <h2 className="text-lg font-bold text-foreground">
            {monthNames[month]} {year}
          </h2>
          <button onClick={nextMonth} className="touch-target p-2 rounded-lg active:bg-card">
            <ChevronRight size={24} />
          </button>
        </div>

        {/* Month stats */}
        <div className="flex gap-3 mb-3">
          <div className="flex-1 rounded-xl bg-primary/10 py-2 text-center">
            <span className="text-lg font-bold text-primary">{presentCount}</span>
            <span className="text-[10px] ml-1 text-muted-foreground">{t("present")}</span>
          </div>
          <div className="flex-1 rounded-xl bg-destructive/10 py-2 text-center">
            <span className="text-lg font-bold text-destructive">{absentCount}</span>
            <span className="text-[10px] ml-1 text-muted-foreground">{t("absent")}</span>
          </div>
        </div>

        <p className="text-[10px] text-center text-muted-foreground mb-2">{t("tapToEdit")}</p>

        {/* Day Labels */}
        <div className="grid grid-cols-7 gap-1 mb-2">
          {dayLabels.map((d) => (
            <div key={d} className="text-center text-xs font-semibold text-muted-foreground py-1">
              {d}
            </div>
          ))}
        </div>

        {/* Calendar Grid */}
        <div className="grid grid-cols-7 gap-1">
          {Array.from({ length: firstDayOfWeek }).map((_, i) => (
            <div key={`empty-${i}`} />
          ))}
          {Array.from({ length: daysInMonth }, (_, i) => {
            const day = i + 1;
            const data = dayMap[day];
            const isPresent = data?.status === "present";
            const isAbsent = data?.status === "absent";
            const isHalf = data?.type === "half";
            const hasAdvance = data?.advance_amount && data.advance_amount > 0;
            const isToday = isCurrentMonth && day === todayDate.getDate();
            return (
              <button
                key={day}
                onClick={() => handleDayClick(day)}
                className={`aspect-square flex flex-col items-center justify-center rounded-full text-sm font-bold transition-colors relative active:scale-90 ${
                  isPresent
                    ? "bg-primary text-primary-foreground"
                    : isAbsent
                    ? "bg-destructive text-destructive-foreground"
                    : isToday
                    ? "border-2 border-primary text-foreground"
                    : "text-foreground/60"
                }`}
              >
                {day}
                {isHalf && (
                  <div className="absolute top-1 right-2 w-1.5 h-1.5 rounded-full bg-accent-foreground" />
                )}
                {hasAdvance && (
                  <div className="absolute bottom-1 w-2 h-2 rounded-full bg-orange-500 shadow-[0_0_4px_rgba(249,115,22,0.6)]" />
                )}
              </button>
            );
          })}
        </div>

        {/* Legend */}
        <div className="flex gap-4 mt-4 justify-center">
          <div className="flex items-center gap-1.5">
            <div className="h-3 w-3 rounded-full bg-primary" />
            <span className="text-[10px] font-medium text-muted-foreground">{t("present")}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <div className="h-3 w-3 rounded-full bg-destructive" />
            <span className="text-[10px] font-medium text-muted-foreground">{t("absent")}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <div className="h-3 w-3 rounded-full bg-orange-500" />
            <span className="text-[10px] font-medium text-muted-foreground">Advance</span>
          </div>
        </div>
      </div>

      {/* Edit Dialog */}
      <AttendanceEditDialog
        open={showEditDialog}
        onOpenChange={setShowEditDialog}
        title={selectedDay ? `${selectedDay} ${monthNames[month]} ${year}` : "Edit Attendance"}
        initialData={initialDialogData}
        onSave={saveDay}
        onDelete={deleteDay}
        showDelete={selectedDay !== null && !!dayMap[selectedDay]}
      />


    </div>
  );
};

export default CalendarPage;
