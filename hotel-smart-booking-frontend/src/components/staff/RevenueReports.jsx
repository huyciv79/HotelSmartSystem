import { useState } from 'react';

const RevenueReports = () => {
  return (
    <div className="space-y-6 animate-scale-in text-left">
      <div className="border-b border-neutral-900 pb-4">
        <h3 className="text-white font-black text-base uppercase tracking-wider m-0">BÁO CÁO DOANH THU & HIỆU SUẤT</h3>
        <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Phân tích tình hình tài chính và doanh thu đặt phòng</p>
      </div>

      {/* Revenue grid charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Column chart with raw CSS */}
        <div className="lg:col-span-2 bg-[#0f0f12] border border-neutral-900 p-6 shadow-md">
          <h4 className="text-xs font-black text-white uppercase tracking-widest border-b border-neutral-850 pb-3 mb-6">
            Biểu đồ doanh thu 6 tháng gần nhất
          </h4>

          <div className="h-64 flex items-end justify-between gap-4 pt-4 px-2">
            {[
              { month: 'T1', rev: '98M', val: 65 },
              { month: 'T2', rev: '120M', val: 80 },
              { month: 'T3', rev: '85M', val: 55 },
              { month: 'T4', rev: '110M', val: 72 },
              { month: 'T5', rev: '147M', val: 95 },
              { month: 'T6', rev: '160M', val: 100 },
            ].map((item, idx) => (
              <div key={idx} className="flex-1 flex flex-col items-center gap-2">
                <span className="text-[9px] text-primary font-black">{item.rev}</span>
                <div
                  className="w-full bg-primary hover:bg-white transition-all duration-300 shadow-md"
                  style={{ height: `${item.val * 1.8}px` }}
                />
                <span className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">{item.month}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Occupancy card */}
        <div className="bg-neutral-950 border border-neutral-900 p-6 shadow-md flex flex-col justify-between">
          <div>
            <h4 className="text-xs font-black uppercase tracking-widest text-primary border-b border-neutral-900 pb-3 mb-4">Hiệu suất phòng nghỉ</h4>
            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider leading-relaxed">Phân phối tỷ lệ lấp đầy giữa các phân khúc khách hàng.</p>
          </div>

          <div className="space-y-4 py-4">
            <div>
              <div className="flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1 text-slate-400">
                <span>Đoàn / Sự kiện (Group)</span>
                <span className="text-white">42%</span>
              </div>
              <div className="w-full h-1.5 bg-neutral-900"><div className="bg-primary h-full" style={{ width: '42%' }}></div></div>
            </div>

            <div>
              <div className="flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1 text-slate-400">
                <span>Đặt phòng trực tuyến (Online)</span>
                <span className="text-white">48%</span>
              </div>
              <div className="w-full h-1.5 bg-neutral-900"><div className="bg-blue-600 h-full" style={{ width: '48%' }}></div></div>
            </div>

            <div>
              <div className="flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1 text-slate-400">
                <span>Đặt phòng trực tiếp (Walk-in)</span>
                <span className="text-white">10%</span>
              </div>
              <div className="w-full h-1.5 bg-neutral-900"><div className="bg-green-600 h-full" style={{ width: '10%' }}></div></div>
            </div>
          </div>

          <div className="text-[9px] text-slate-600 font-bold uppercase tracking-wider border-t border-neutral-900 pt-3">
            Dữ liệu được làm mới tự động mỗi 12 giờ.
          </div>
        </div>
      </div>
    </div>
  );
};

export default RevenueReports;
