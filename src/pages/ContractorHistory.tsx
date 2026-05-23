import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs } from "firebase/firestore";
import { FileText, FileSpreadsheet, ChevronLeft, ChevronRight, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import jsPDF from "jspdf";

export const ContractorHistory = () => {
  const { user, userData } = useAuth();
  const [workers, setWorkers] = useState<any[]>([]);
  const [records, setRecords] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedMonth, setSelectedMonth] = useState(new Date());

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];

  useEffect(() => {
    if (user) loadHistory();
  }, [user, selectedMonth]);

  const loadHistory = async () => {
    if (!user) return;
    const year = selectedMonth.getFullYear();
    const month = String(selectedMonth.getMonth() + 1).padStart(2, "0");
    const yearMonth = `${year}-${month}`;

    try {
      const wQ = query(collection(db, "workers"), where("contractorId", "==", user.uid));
      const wSnap = await getDocs(wQ);

      const workersMap: Record<string, any> = {};
      wSnap.docs.forEach(d => {
        workersMap[`worker_${d.id}`] = d.data();
      });

      const allRecords: any[] = [];

      if (wSnap.size > 0) {
        const attQ = query(collection(db, "attendance"), where("contractorId", "==", user.uid));
        const attSnap = await getDocs(attQ);

        attSnap.docs.forEach((doc) => {
          const data = doc.data();
          if (data.date.startsWith(yearMonth)) {
             const workerData = workersMap[data.user_id] || { name: 'Unknown', workType: 'Unknown' };
             allRecords.push({
                 id: doc.id,
                 workerName: workerData.name,
                 workerRole: workerData.workType || 'Labour',
                 date: data.date,
                 status: data.status,
                 type: data.type,
                 overtime_hours: data.overtime_hours,
                 advance_amount: data.advance_amount,
                 note: data.note
             });
          }
        });
      }

      allRecords.sort((a, b) => b.date.localeCompare(a.date));
      setRecords(allRecords);
    } catch (err) {
      console.error("Error loading contractor history:", err);
    }
  };

  const prevMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() - 1, 1));
  const nextMonth = () => setSelectedMonth(new Date(selectedMonth.getFullYear(), selectedMonth.getMonth() + 1, 1));

  const filteredRecords = records.filter(r =>
      r.workerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.date.includes(searchQuery) ||
      (r.note && r.note.toLowerCase().includes(searchQuery))
  );

  const exportPDF = () => {
    const pdf = new jsPDF("p", "mm", "a4");
    const pageW = pdf.internal.pageSize.getWidth();
    const userName = userData?.name || "Contractor";
    const monthLabel = `${monthNames[selectedMonth.getMonth()]} ${selectedMonth.getFullYear()}`;

    // Header Background
    pdf.setFillColor(15, 23, 42); // slate-900
    pdf.rect(0, 0, pageW, 35, "F");

    // Header Text
    pdf.setTextColor(255, 255, 255);
    pdf.setFontSize(24);
    pdf.setFont("helvetica", "bold");
    pdf.text("DailyWork", 14, 20);

    pdf.setFontSize(11);
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(148, 163, 184); // slate-400
    pdf.text(`${userName} | Global Log - ${monthLabel}`, 14, 28);

    pdf.setFontSize(9);
    pdf.text(`Generated: ${new Date().toLocaleDateString()}`, pageW - 45, 28);

    // Table Setup
    let y = 45;
    pdf.setFillColor(241, 245, 249); // slate-100
    pdf.rect(14, y, pageW - 28, 10, "F");
    pdf.setTextColor(71, 85, 105); // slate-600
    pdf.setFontSize(9);
    pdf.setFont("helvetica", "bold");

    const cols = [18, 40, 80, 110, 140];
    const headers = ["Date", "Worker", "Status/Type", "Advance", "Note"];
    headers.forEach((h, i) => pdf.text(h, cols[i], y + 7));

    y += 13;
    pdf.setFont("helvetica", "normal");

    filteredRecords.forEach((r, idx) => {
      if (y > 270) {
        pdf.addPage();
        y = 15;
        pdf.setFillColor(241, 245, 249);
        pdf.rect(14, y, pageW - 28, 10, "F");
        pdf.setTextColor(71, 85, 105);
        pdf.setFontSize(9);
        pdf.setFont("helvetica", "bold");
        headers.forEach((h, i) => pdf.text(h, cols[i], y + 7));
        y += 13;
        pdf.setFont("helvetica", "normal");
      }

      if (idx % 2 === 0) {
        pdf.setFillColor(248, 250, 252);
        pdf.rect(14, y - 4, pageW - 28, 9, "F");
      }

      pdf.setTextColor(51, 65, 85);
      pdf.setFontSize(9);

      pdf.text(r.date.split("-").reverse().join("/"), cols[0], y + 2);

      pdf.setFont("helvetica", "bold");
      pdf.text(r.workerName.substring(0, 15), cols[1], y + 2);
      pdf.setFont("helvetica", "normal");

      let typeStr = r.status;
      if (r.status === 'present') typeStr = r.type === 'half' ? 'Half Day' : 'Present';
      if (r.status === 'absent') typeStr = 'Absent';
      if (r.status === 'advance') typeStr = '-';

      if (r.status === 'present') pdf.setTextColor(22, 163, 74);
      else if (r.status === 'absent') pdf.setTextColor(220, 38, 38);
      else pdf.setTextColor(51, 65, 85);

      pdf.text(typeStr, cols[2], y + 2);
      pdf.setTextColor(51, 65, 85);

      if (r.advance_amount) {
        pdf.setTextColor(249, 115, 22);
        pdf.text(`Rs. ${r.advance_amount}`, cols[3], y + 2);
        pdf.setTextColor(51, 65, 85);
      } else {
        pdf.text("-", cols[3], y + 2);
      }

      pdf.text((r.note || "-").substring(0, 25), cols[4], y + 2);

      y += 9;
    });

    pdf.save(`Contractor_Global_Log_${monthLabel.replace(" ", "_")}.pdf`);
  };

  return (
    <div className="space-y-4 animate-in fade-in">
        {/* Month selector */}
        <div className="flex items-center justify-between mb-3 bg-card border border-border rounded-xl p-2">
          <button onClick={prevMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronLeft size={20} />
          </button>
          <span className="text-sm font-bold text-foreground">
            {monthNames[selectedMonth.getMonth()]} {selectedMonth.getFullYear()}
          </span>
          <button onClick={nextMonth} className="p-2 rounded-lg active:bg-muted">
            <ChevronRight size={20} />
          </button>
        </div>

        {/* Search & Export */}
        <div className="flex gap-2 mb-3">
          <div className="relative flex-1">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search..."
              className="pl-9 h-11 rounded-xl"
            />
          </div>
          <Button variant="outline" className="h-11 rounded-xl shrink-0" onClick={exportPDF}>
            <FileText size={16} />
          </Button>
        </div>

        <div>
            {filteredRecords.length === 0 ? (
                <div className="text-center py-10 text-muted-foreground text-sm bg-card rounded-2xl border border-dashed border-border">No records found for this month.</div>
            ) : (
                <div className="space-y-3">
                    {filteredRecords.map((r, i) => (
                        <div key={i} className="bg-card p-4 rounded-2xl border border-border flex justify-between items-center">
                            <div>
                                <p className="font-bold text-sm">{r.workerName}</p>
                                <div className="flex gap-2 items-center mt-1">
                                    <span className="text-[10px] text-muted-foreground font-medium">{r.date}</span>
                                    <span className="text-[10px] bg-secondary px-2 py-0.5 rounded-full">{r.workerRole}</span>
                                </div>
                                {r.note && <p className="text-[10px] text-muted-foreground mt-1 truncate max-w-[200px]">{r.note}</p>}
                            </div>
                            <div className="flex flex-col items-end gap-1.5">
                                {r.status !== 'advance' && (
                                    <span className={`text-[10px] font-bold px-2 py-1 rounded-full ${r.status === 'present' ? 'bg-green-500/10 text-green-600' : 'bg-red-500/10 text-red-600'}`}>
                                        {r.status === 'present' ? (r.type === 'half' ? 'Half Day' : 'Present') : 'Absent'}
                                    </span>
                                )}
                                {(r.advance_amount || 0) > 0 && (
                                    <span className="text-[10px] font-bold px-2 py-1 rounded-full bg-orange-500/10 text-orange-600">
                                        ₹{r.advance_amount} Advance
                                    </span>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    </div>
  );
};
