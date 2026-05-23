import { useState, useEffect } from "react";
import { useLanguage } from "@/contexts/LanguageContext";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

export interface AttendanceEditData {
  status: string;
  type: string;
  reason: string;
  overtime_hours: number;
  note: string;
  advance_amount: number;
}

interface AttendanceEditDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  initialData?: Partial<AttendanceEditData>;
  onSave: (data: AttendanceEditData) => void;
  onDelete?: () => void;
  showDelete?: boolean;
}

export const AttendanceEditDialog = ({
  open,
  onOpenChange,
  title,
  initialData,
  onSave,
  onDelete,
  showDelete = false,
}: AttendanceEditDialogProps) => {
  const { t } = useLanguage();

  const [editStatus, setEditStatus] = useState("present");
  const [editType, setEditType] = useState("full");
  const [editReason, setEditReason] = useState("sick");
  const [editNote, setEditNote] = useState("");
  const [editOT, setEditOT] = useState(0);
  const [editAdvance, setEditAdvance] = useState(0);

  const absenceReasons = ["sick", "personal", "holiday", "weather", "other"];

  useEffect(() => {
    if (open) {
      setEditStatus(initialData?.status === "advance" ? "present" : (initialData?.status || "present"));
      setEditType(initialData?.type || "full");
      setEditReason(initialData?.reason || "sick");
      setEditNote(initialData?.note || "");
      setEditOT(initialData?.overtime_hours || 0);
      setEditAdvance(initialData?.advance_amount || 0);
    }
  }, [open, initialData]);

  const handleSave = () => {
    onSave({
      status: editStatus,
      type: editType,
      reason: editReason,
      overtime_hours: editOT,
      note: editNote,
      advance_amount: editAdvance,
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm mx-auto">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{t("tapToEdit")}</DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          {/* Status toggle */}
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={() => setEditStatus("present")}
              className={`rounded-xl py-3 text-sm font-bold transition-all active:scale-95 ${
                editStatus === "present" ? "bg-primary text-primary-foreground" : "bg-muted text-foreground"
              }`}
            >
              {t("present")}
            </button>
            <button
              onClick={() => setEditStatus("absent")}
              className={`rounded-xl py-3 text-sm font-bold transition-all active:scale-95 ${
                editStatus === "absent" ? "bg-destructive text-destructive-foreground" : "bg-muted text-foreground"
              }`}
            >
              {t("absent")}
            </button>
          </div>

          {editStatus === "present" && (
            <>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setEditType("full")}
                  className={`rounded-xl py-2 text-xs font-bold ${editType === "full" ? "bg-accent border-2 border-primary" : "bg-muted"}`}
                >
                  {t("fullDay")}
                </button>
                <button
                  onClick={() => setEditType("half")}
                  className={`rounded-xl py-2 text-xs font-bold ${editType === "half" ? "bg-accent border-2 border-primary" : "bg-muted"}`}
                >
                  {t("halfDay")}
                </button>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">{t("overtime")}</span>
                <div className="flex items-center gap-2">
                  <button onClick={() => setEditOT(Math.max(0, editOT - 1))} className="h-7 w-7 rounded-full bg-muted flex items-center justify-center text-sm font-bold">-</button>
                  <span className="font-bold w-4 text-center">{editOT}</span>
                  <button onClick={() => setEditOT(editOT + 1)} className="h-7 w-7 rounded-full bg-muted flex items-center justify-center text-sm font-bold">+</button>
                </div>
              </div>
            </>
          )}

          {editStatus === "absent" && (
            <div className="grid grid-cols-2 gap-2">
              {absenceReasons.map((r) => (
                <button
                  key={r}
                  onClick={() => setEditReason(r)}
                  className={`rounded-xl px-2 py-2 text-xs font-semibold ${
                    editReason === r ? "bg-destructive text-destructive-foreground" : "bg-muted"
                  }`}
                >
                  {t(r)}
                </button>
              ))}
            </div>
          )}

          <div className="flex flex-col gap-1.5 pt-2">
            <span className="text-sm font-medium">Advance Payment (₹)</span>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-bold">₹</span>
              <input
                type="number"
                value={editAdvance || ""}
                onChange={(e) => setEditAdvance(Math.max(0, parseInt(e.target.value) || 0))}
                placeholder="0"
                className="w-full rounded-xl border border-border bg-background px-8 py-2.5 text-base font-bold text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
          </div>

          <Textarea
            value={editNote}
            onChange={(e) => setEditNote(e.target.value)}
            placeholder={t("addNote") + "..."}
            className="min-h-[50px]"
          />
        </div>
        <DialogFooter className="flex gap-2">
          {showDelete && onDelete && (
            <Button variant="destructive" size="sm" onClick={onDelete}>
              {t("removeAttendance")}
            </Button>
          )}
          <Button onClick={handleSave}>{t("save")}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
