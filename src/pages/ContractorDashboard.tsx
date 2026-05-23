import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { db } from "@/lib/firebase";
import { collection, query, where, getDocs } from "firebase/firestore";
import { Users, UserCheck, Wallet, ArrowRight, IndianRupee, Plus, Bell, TrendingUp, Crown } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { doc, setDoc, serverTimestamp } from "firebase/firestore";

export const ContractorDashboard = () => {
  const { user, userData } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({ totalWorkers: 0, todayPresent: 0, totalPaid: 0, pendingAmount: 0 });
  const [workers, setWorkers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  // Payment State
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [paymentForm, setPaymentForm] = useState({ workerId: "", amount: "", date: new Date().toISOString().split("T")[0] });
  const [savingPayment, setSavingPayment] = useState(false);

  useEffect(() => {
    if (user) loadStats();
  }, [user]);

  const loadStats = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const wQ = query(collection(db, "workers"), where("contractorId", "==", user.uid));
      const workersSnap = await getDocs(wQ);

      const workersMap: Record<string, number> = {};
      const wList: any[] = [];
      workersSnap.docs.forEach(d => {
        workersMap[`worker_${d.id}`] = d.data().wage || 0;
        wList.push({ id: d.id, ...d.data() });
      });

      setWorkers(wList);
      const workerCount = workersSnap.size;

      let presentToday = 0;
      let totalPaid = 0;
      let totalEarned = 0;

      const todayStr = new Date().toISOString().split("T")[0];
      const monthStr = todayStr.substring(0, 7);

      if (workerCount > 0) {
        // Fetch all attendance for this contractor in one query
        const attQ = query(collection(db, "attendance"), where("contractorId", "==", user.uid));
        const attSnap = await getDocs(attQ);

        attSnap.docs.forEach((doc) => {
          const data = doc.data();
          const wage = workersMap[data.user_id] || 0;

          if (data.date === todayStr && data.status === "present") {
            presentToday++;
          }
          if (data.date.startsWith(monthStr)) {
            if (data.status === "present") {
               totalEarned += wage;
               if (data.type === "half") totalEarned -= wage / 2;
            }
            if (data.advance_amount) {
               totalPaid += data.advance_amount;
            }
          }
        });
      }

      setStats({
        totalWorkers: workerCount,
        todayPresent: presentToday,
        totalPaid: totalPaid,
        pendingAmount: Math.max(0, totalEarned - totalPaid)
      });
    } catch (err) {
      console.error("Error loading contractor stats:", err);
    }
    setLoading(false);
  };

  const handleSavePayment = async () => {
    if (!user || !paymentForm.workerId || !paymentForm.amount) return;
    setSavingPayment(true);

    try {
      const docId = `worker_${paymentForm.workerId}_${paymentForm.date}_advance`;
      const amount = Number(paymentForm.amount);

      // Look for existing advance to merge (optional, simple overwrite for now if same day)
      const existingDoc = await getDocs(query(
        collection(db, "attendance"),
        where("user_id", "==", `worker_${paymentForm.workerId}`),
        where("date", "==", paymentForm.date),
        where("status", "==", "advance")
      ));

      let newAmount = amount;
      if (!existingDoc.empty) {
        newAmount += existingDoc.docs[0].data().advance_amount || 0;
      }

      await setDoc(doc(db, "attendance", docId), {
        user_id: `worker_${paymentForm.workerId}`,
        contractorId: user.uid,
        date: paymentForm.date,
        status: "advance",
        advance_amount: newAmount,
        note: "Advance Payment",
        timestamp: serverTimestamp(),
      });

      setShowPaymentModal(false);
      setPaymentForm({ workerId: "", amount: "", date: new Date().toISOString().split("T")[0] });
      loadStats();
    } catch (err) {
      console.error("Error saving payment:", err);
    }
    setSavingPayment(false);
  };

  const initials = (userData?.name || "C")
    .split(" ")
    .map((w: string) => w[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);

  return (
    <div className="space-y-6 animate-in fade-in w-full">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Avatar className="h-12 w-12 border-2 border-primary/20">
            <AvatarImage src={user?.photoURL || ""} alt={userData?.name || "Contractor"} />
            <AvatarFallback className="bg-primary/10 text-primary text-sm font-bold">
              {initials}
            </AvatarFallback>
          </Avatar>
          <div>
            <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">Hi, {userData?.name?.split(" ")[0] || "Contractor"} <span className="animate-bounce">👋</span></h1>
            <p className="text-sm text-muted-foreground mt-1">Manage your workforce</p>
          </div>
        </div>
        <button onClick={() => navigate('/premium')} className="relative h-12 w-12 rounded-full bg-gradient-to-br from-amber-100 to-orange-100 dark:from-amber-900/40 dark:to-orange-900/40 border border-amber-200 dark:border-amber-800 flex items-center justify-center hover: active:scale-95 transition-all ">
          <Crown size={22} className="text-amber-600 dark:text-amber-500" />
        </button>
      </div>
            {/* Mini Analytics Preview */}
      <div className="bg-gradient-to-r from-primary/10 to-indigo-500/10 border border-primary/20 rounded-2xl p-4 flex items-center justify-between ">
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-full bg-primary/20 flex items-center justify-center shrink-0">
             <TrendingUp size={20} className="text-primary" />
          </div>
          <div>
             <h3 className="text-sm font-bold text-foreground">Weekly Performance</h3>
             <p className="text-xs text-muted-foreground font-medium mt-0.5"><span className="text-green-600 font-bold">+12%</span> active workers vs last week</p>
          </div>
        </div>
        <button onClick={() => navigate('/stats')} className="text-xs font-bold text-primary bg-primary/10 hover:bg-primary/20 px-4 py-2 rounded-xl transition-colors">
          View
        </button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-card p-5 rounded-2xl border border-border  hover: transition-shadow">
          <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center mb-3">
            <Users size={20} className="text-primary" />
          </div>
          <p className="text-3xl font-bold text-foreground">{loading ? <div className="h-8 w-16 bg-muted animate-pulse rounded"></div> : stats.totalWorkers}</p>
          <p className="text-xs font-medium text-muted-foreground mt-1">Total Workers</p>
        </div>

        <div className="bg-card p-5 rounded-2xl border border-border  hover: transition-shadow">
          <div className="h-10 w-10 rounded-full bg-green-500/10 flex items-center justify-center mb-3">
            <UserCheck size={20} className="text-green-500" />
          </div>
          <p className="text-3xl font-bold text-foreground">{loading ? <div className="h-8 w-16 bg-muted animate-pulse rounded"></div> : stats.todayPresent}</p>
          <p className="text-xs font-medium text-muted-foreground mt-1">Today's Attendance</p>
        </div>

        <div className="bg-card p-5 rounded-2xl border border-border  hover: transition-shadow">
          <div className="h-10 w-10 rounded-full bg-blue-500/10 flex items-center justify-center mb-3">
            <Wallet size={20} className="text-blue-500" />
          </div>
          <p className="text-2xl font-bold text-foreground">{loading ? <div className="h-8 w-20 bg-muted animate-pulse rounded"></div> : `₹${stats.totalPaid.toLocaleString()}`}</p>
          <p className="text-xs font-medium text-muted-foreground mt-1">Total Paid (Month)</p>
        </div>

        <div className="bg-card p-5 rounded-2xl border border-border  hover: transition-shadow">
          <div className="h-10 w-10 rounded-full bg-orange-500/10 flex items-center justify-center mb-3">
            <Wallet size={20} className="text-orange-500" />
          </div>
          <p className="text-2xl font-bold text-primary">{loading ? <div className="h-8 w-20 bg-muted animate-pulse rounded"></div> : `₹${stats.pendingAmount.toLocaleString()}`}</p>
          <p className="text-xs font-medium text-muted-foreground mt-1">Pending Amount</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-8">
        <h2 className="text-sm font-bold text-muted-foreground uppercase tracking-wider">Quick Actions</h2>

        <button onClick={() => navigate('/workers')} className="w-full bg-card border border-border p-5 rounded-2xl flex items-center justify-between hover: active:scale-[0.98] transition-all">
          <div className="flex items-center gap-4">
            <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
              <Users size={20} className="text-primary" />
            </div>
            <div className="text-left">
              <p className="font-bold text-base text-foreground">Manage Workers</p>
              <p className="text-xs text-muted-foreground mt-0.5">Add, edit or remove workers</p>
            </div>
          </div>
          <ArrowRight size={20} className="text-muted-foreground" />
        </button>

        <button onClick={() => navigate('/calendar')} className="w-full bg-card border border-border p-5 rounded-2xl flex items-center justify-between hover: active:scale-[0.98] transition-all">
          <div className="flex items-center gap-4">
            <div className="h-12 w-12 rounded-full bg-green-500/10 flex items-center justify-center">
              <UserCheck size={20} className="text-green-500" />
            </div>
            <div className="text-left">
              <p className="font-bold text-base text-foreground">Mark Attendance</p>
              <p className="text-xs text-muted-foreground mt-0.5">Daily attendance for all workers</p>
            </div>
          </div>
          <ArrowRight size={20} className="text-muted-foreground" />
        </button>

        <button onClick={() => setShowPaymentModal(true)} className="w-full bg-card border border-border p-5 rounded-2xl flex items-center justify-between hover: active:scale-[0.98] transition-all">
          <div className="flex items-center gap-4">
            <div className="h-12 w-12 rounded-full bg-orange-500/10 flex items-center justify-center">
              <IndianRupee size={20} className="text-orange-500" />
            </div>
            <div className="text-left">
              <p className="font-bold text-base text-foreground">Add Advance Payment</p>
              <p className="text-xs text-muted-foreground mt-0.5">Record payments for workers</p>
            </div>
          </div>
          <Plus size={20} className="text-muted-foreground" />
        </button>
      </div>

      <Dialog open={showPaymentModal} onOpenChange={setShowPaymentModal}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader>
            <DialogTitle>Add Advance Payment</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 mt-2">
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1 block">Select Worker *</label>
              <Select value={paymentForm.workerId} onValueChange={(v) => setPaymentForm({ ...paymentForm, workerId: v })}>
                <SelectTrigger className="h-11 rounded-xl"><SelectValue placeholder="Select Worker" /></SelectTrigger>
                <SelectContent>
                  {workers.map(w => (
                    <SelectItem key={w.id} value={w.id}>{w.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1 block">Amount (₹) *</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-bold">₹</span>
                <Input type="number" value={paymentForm.amount} onChange={(e) => setPaymentForm({ ...paymentForm, amount: e.target.value })} className="pl-8 h-11 rounded-xl" placeholder="0" />
              </div>
            </div>
            <div>
              <label className="text-xs font-bold text-muted-foreground mb-1 block">Date</label>
              <Input type="date" value={paymentForm.date} onChange={(e) => setPaymentForm({ ...paymentForm, date: e.target.value })} className="h-11 rounded-xl" />
            </div>
          </div>
          <DialogFooter className="mt-6 flex gap-2">
            <Button variant="outline" className="flex-1 rounded-xl" onClick={() => setShowPaymentModal(false)}>Cancel</Button>
            <Button className="flex-1 rounded-xl" onClick={handleSavePayment} disabled={savingPayment || !paymentForm.workerId || !paymentForm.amount}>
              {savingPayment ? "Saving..." : "Save Payment"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
