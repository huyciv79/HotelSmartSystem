const StatCard = ({
  icon: Icon,
  value,
  label,
  badge,
  badgeColor,
  accentColor,
  accentBg,
  hasBorderTop,
}) => {
  return (
    <div
      className={`p-6 bg-white rounded-xl shadow-[0px_1px_2px_0px_rgba(0,0,0,0.05)] ${
        hasBorderTop ? "border-t-2 border-amber-200" : ""
      }`}
    >
      <div className="flex justify-between items-start">
        <div className={`p-3 rounded-lg ${accentBg}`}>
          <Icon className={`size-5 ${accentColor}`} />
        </div>
        <span className={`text-xs font-bold font-['Geist'] ${badgeColor}`}>
          {badge}
        </span>
      </div>
      <div className="pt-4 text-black text-2xl font-bold font-['Geist']">
        {value}
      </div>
      <div className="text-zinc-700 text-base font-normal font-['Geist']">
        {label}
      </div>
    </div>
  );
};

export default StatCard;
