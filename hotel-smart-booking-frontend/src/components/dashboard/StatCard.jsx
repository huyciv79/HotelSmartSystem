const StatCard = ({
  icon: Icon,
  value,
  label,
  badge,
  badgeColor,
  hasBorderTop,
}) => {
  return (
    <div
      className={`p-6 bg-white rounded-none border border-outline-variant shadow-sm font-['Montserrat'] text-left ${
        hasBorderTop ? "border-t-4 border-primary" : ""
      }`}
    >
      <div className="flex justify-between items-start">
        <div className={`p-3 rounded-none bg-primary/5`}>
          <Icon className={`size-5 text-primary`} />
        </div>
        <span className={`text-[10px] font-black uppercase tracking-wider ${badgeColor}`}>
          {badge}
        </span>
      </div>
      <div className="pt-4 text-black text-2xl font-black">
        {value}
      </div>
      <div className="text-zinc-500 text-xs font-bold uppercase tracking-wider mt-1">
        {label === "Upcoming Stays" ? "Đặt phòng sắp tới" : label === "Completed Stays" ? "Kỳ nghỉ đã hoàn thành" : label === "Pending Requests" ? "Yêu cầu đang chờ" : label === "Total Spending" ? "Tổng chi tiêu" : label}
      </div>
    </div>
  );
};

export default StatCard;
