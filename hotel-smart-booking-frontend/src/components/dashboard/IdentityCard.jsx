import { useState, useEffect } from "react";
import { ShieldCheck, ShieldAlert, ShieldX, Clock, CreditCard, Smartphone, Loader2 } from "lucide-react";
import { getEkycProfile } from "../../services/ekycService";
import { useLanguage } from "../../context/LanguageContext";

const IdentityCard = ({ onNavigate }) => {
  const { t } = useLanguage();
  const [ekyc, setEkyc] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchEkyc = async () => {
      try {
        const token = localStorage.getItem("accessToken");
        if (token) {
          const res = await getEkycProfile();
          setEkyc(res?.data || null);
        }
      } catch (err) {
        console.error("Failed to load eKYC profile:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchEkyc();
  }, []);

  if (loading) {
    return (
      <div className="p-8 bg-[#111111] rounded-none border border-neutral-800 border-l-4 border-l-primary shadow-xl flex items-center justify-center font-['Montserrat'] min-h-[200px]">
        <Loader2 className="text-primary size-8 animate-spin" />
      </div>
    );
  }

  const status = ekyc?.status || "NOT_FOUND";
  const verifiedAt = ekyc?.verifiedAt;

  // Render content based on status
  const getContent = () => {
    switch (status) {
      case "VERIFIED":
        return {
          title: t("ekyc_status_verified_title", "ĐÃ XÁC MINH DANH TÍNH"),
          desc: t("ekyc_status_verified_desc", "Đã kích hoạt FaceID Express Check-in và khóa phòng kỹ thuật số."),
          icon: <ShieldCheck className="text-emerald-500 size-6 animate-pulse" />,
          border: "border-l-emerald-500",
          footer: verifiedAt
            ? `${t("ekyc_status_verified_at", "ĐÃ XÁC MINH VÀO")} ${new Date(verifiedAt).toLocaleDateString("vi-VN")}`
            : t("ekyc_status_verified_success", "ĐÃ XÁC MINH THÀNH CÔNG"),
          button: null,
        };
      case "PENDING":
        return {
          title: t("ekyc_status_pending_title", "HỒ SƠ CHỜ DUYỆT"),
          desc: t("ekyc_status_pending_desc", "Hồ sơ xác minh eKYC đang được xử lý. Vui lòng chờ trong giây lát."),
          icon: <Clock className="text-amber-500 size-6" />,
          border: "border-l-amber-500",
          footer: t("ekyc_status_pending_footer", "ĐANG CHỜ HỆ THỐNG XÁC THỰC"),
          button: (
            <button
              onClick={onNavigate}
              className="mt-2 w-full py-2 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 font-bold uppercase text-[9px] tracking-widest transition-all cursor-pointer rounded-none"
            >
              {t("ekyc_btn_view_progress", "Xem Tiến Trình")}
            </button>
          ),
        };
      case "REJECTED":
        return {
          title: t("ekyc_status_rejected_title", "XÁC MINH THẤT BẠI"),
          desc: t("ekyc_status_rejected_desc", "Hồ sơ eKYC bị từ chối do hình ảnh mờ hoặc thông tin không trùng khớp."),
          icon: <ShieldX className="text-red-500 size-6" />,
          border: "border-l-red-500",
          footer: ekyc?.rejectionReason?.toUpperCase() || t("ekyc_status_rejected_footer", "HỒ SƠ BỊ TỪ CHỐI"),
          button: (
            <button
              onClick={onNavigate}
              className="mt-2 w-full py-2 bg-red-500/15 hover:bg-red-500/25 text-red-400 border border-red-500/30 font-bold uppercase text-[9px] tracking-widest transition-all cursor-pointer rounded-none"
            >
              {t("ekyc_btn_update_now", "Cập nhật eKYC ngay")}
            </button>
          ),
        };
      case "NOT_FOUND":
      default:
        return {
          title: t("ekyc_status_not_found_title", "CHƯA XÁC MINH DANH TÍNH"),
          desc: t("ekyc_status_not_found_desc", "Hãy hoàn thành eKYC để kích hoạt FaceID và check-in không cần quầy lễ tân."),
          icon: <ShieldAlert className="text-neutral-500 size-6" />,
          border: "border-l-primary",
          footer: t("ekyc_status_not_found_footer", "THƯỜNG MẤT DƯỚI 2 PHÚT"),
          button: (
            <button
              onClick={onNavigate}
              className="mt-2 w-full py-3 bg-primary hover:bg-white hover:text-black text-white font-black uppercase text-[10px] tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 parallelogram-btn"
            >
              {t("ekyc_btn_verify_now", "Xác minh eKYC ngay")}
            </button>
          ),
        };
    }
  };

  const current = getContent();

  return (
    <div className={`p-8 bg-[#111111] rounded-none border border-neutral-800 border-l-4 ${current.border} shadow-xl relative overflow-hidden font-['Montserrat']`}>
      <div className="absolute size-40 bg-primary rounded-full blur-[48px] opacity-10 left-[170px] top-[169px]" />

      <div className="relative z-10 flex flex-col gap-6">
        <div className="flex justify-between items-start">
          <h2 className="text-white font-bold text-sm uppercase tracking-wider leading-relaxed pr-2">
            {current.title}
          </h2>
          {current.icon}
        </div>

        <p className="text-neutral-400 text-xs font-medium leading-relaxed uppercase tracking-wider">
          {current.desc}
        </p>

        {status === "VERIFIED" && (
          <div className="flex gap-3">
            <div className="w-24 h-16 bg-white/5 rounded-none border border-white/10 flex flex-col items-center justify-center hover:border-emerald-500/50 transition-colors gap-1.5">
              <CreditCard className="text-white/60 size-5" />
              <span className="text-[7px] text-white/40 font-bold tracking-widest uppercase">{t("ekyc_label_smart_key", "SMART KEY")}</span>
            </div>
            <div className="w-24 h-16 bg-white/5 rounded-none border border-white/10 flex flex-col items-center justify-center hover:border-emerald-500/50 transition-colors gap-1.5">
              <Smartphone className="text-white/60 size-5" />
              <span className="text-[7px] text-white/40 font-bold tracking-widest uppercase">{t("ekyc_label_e_checkin", "E-CHECKIN")}</span>
            </div>
          </div>
        )}

        {current.button}

        <div className="pt-3 border-t border-white/10">
          <p className="text-white/30 text-[9px] uppercase tracking-widest font-black truncate">
            {current.footer}
          </p>
        </div>
      </div>
    </div>
  );
};

export default IdentityCard;
