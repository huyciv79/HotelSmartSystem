import StatCard from "./StatCard";

const StatsGrid = ({ stats }) => {
  return (
    <div className="grid grid-cols-4 gap-4">
      {stats.map((stat, index) => (
        <StatCard key={index} {...stat} />
      ))}
    </div>
  );
};

export default StatsGrid;
