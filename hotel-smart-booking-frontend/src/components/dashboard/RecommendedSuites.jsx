import { ChevronLeft, ChevronRight } from "lucide-react";
import SuiteCard from "./SuiteCard";
import { useRef } from "react";

const RecommendedSuites = ({ suites }) => {
  const scrollRef = useRef(null);

  const scroll = (direction) => {
    if (scrollRef.current) {
      const scrollAmount = 344;
      scrollRef.current.scrollBy({
        left: direction === "left" ? -scrollAmount : scrollAmount,
        behavior: "smooth",
      });
    }
  };

  return (
    <div className="pb-10 flex flex-col gap-6 font-['Montserrat'] text-left">
      <div className="px-2 flex justify-between items-end">
        <div>
          <h2 className="font-headline-lg text-headline-md text-primary uppercase italic tracking-wider m-0 mb-1">
            Đề xuất dành riêng cho bạn
          </h2>
          <p className="text-secondary text-xs font-bold uppercase tracking-wider">
            Các hạng phòng thượng lưu được tuyển chọn dựa trên sở thích của bạn
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => scroll("left")}
            className="p-2.5 rounded-none border border-outline-variant bg-white hover:bg-slate-100 transition cursor-pointer flex items-center justify-center h-10 w-10"
          >
            <ChevronLeft size={16} className="text-slate-900" />
          </button>
          <button
            onClick={() => scroll("right")}
            className="p-2.5 rounded-none border border-outline-variant bg-white hover:bg-slate-100 transition cursor-pointer flex items-center justify-center h-10 w-10"
          >
            <ChevronRight size={16} className="text-slate-900" />
          </button>
        </div>
      </div>

      <div
        ref={scrollRef}
        className="flex gap-6 overflow-x-auto pb-4 snap-x snap-mandatory"
        style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
      >
        {suites.map((suite, index) => (
          <div key={index} className="snap-start">
            <SuiteCard suite={suite} />
          </div>
        ))}
      </div>
    </div>
  );
};

export default RecommendedSuites;
