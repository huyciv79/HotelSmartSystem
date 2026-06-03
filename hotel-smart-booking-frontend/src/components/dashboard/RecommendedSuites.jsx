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
    <div className="pb-10 flex flex-col gap-6">
      <div className="px-2 flex justify-between items-end">
        <div>
          <h2 className="text-black font-['Playfair_Display']">
            Recommended for You
          </h2>
          <p className="text-zinc-700 text-sm font-['Geist']">
            Exclusive suites curated based on your preferences
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => scroll("left")}
            className="p-2 rounded-full outline outline-1 outline-neutral-300 hover:bg-gray-100 transition"
          >
            <ChevronLeft size={16} className="text-zinc-900" />
          </button>
          <button
            onClick={() => scroll("right")}
            className="p-2 rounded-full outline outline-1 outline-neutral-300 hover:bg-gray-100 transition"
          >
            <ChevronRight size={16} className="text-zinc-900" />
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
