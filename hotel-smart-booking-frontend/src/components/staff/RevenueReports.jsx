import { useState } from 'react';

const RevenueReports = ({ stats }) => {
  // Fallback monthly data if backend stats are not loaded yet
  const defaultMonthlyData = [
    { month: 'T1', rev: '98M', val: 65 },
    { month: 'T2', rev: '120M', val: 80 },
    { month: 'T3', rev: '85M', val: 55 },
    { month: 'T4', rev: '110M', val: 72 },
    { month: 'T5', rev: '147M', val: 95 },
    { month: 'T6', rev: '160M', val: 100 },
  ];

  const monthlyData = stats && stats.monthlyRevenue && stats.monthlyRevenue.length > 0
    ? stats.monthlyRevenue.map(item => ({
        month: item.month,
        rev: item.revenue >= 1000000 
          ? (item.revenue / 1000000).toFixed(0) + 'M' 
          : new Intl.NumberFormat('vi-VN').format(item.revenue),
        val: item.percentage
      }))
    : defaultMonthlyData;

  const defaultTypePercentages = {
    Group: 42.0,
    Online: 48.0,
    'Walk-in': 10.0
  };

  const typePercentages = stats && stats.bookingTypePercentages
    ? stats.bookingTypePercentages
    : defaultTypePercentages;

  const groupPct = typePercentages.Group ?? 0;
  const onlinePct = typePercentages.Online ?? 0;
  const walkinPct = typePercentages['Walk-in'] ?? 0;

  const handleExportCSV = () => {
    if (!stats) return;

    // Build CSV content
    let csvContent = '\uFEFF'; // UTF-8 BOM for Excel support
    csvContent += 'BÁO CÁO THỐNG KÊ ELYSIAN HUB\n';
    csvContent += `Thời gian xuất: ${new Date().toLocaleString('vi-VN')}\n\n`;

    // 1. General Metrics
    csvContent += 'CHỈ SỐ TỔNG QUAN\n';
    csvContent += `Tổng số phòng,${stats.totalRooms}\n`;
    csvContent += `Phòng đang ở,${stats.occupiedRooms}\n`;
    csvContent += `Tỷ lệ lấp đầy,${stats.occupancyRate.toFixed(2)}%\n`;
    csvContent += `Doanh thu dự tính,${stats.expectedRevenue} VND\n`;
    csvContent += `Tổng số lượt Check-in,${stats.totalCheckIns}\n`;
    csvContent += `Tổng số lượt Check-out,${stats.totalCheckOuts}\n`;
    csvContent += `Tổng khách hàng,${stats.totalUniqueCustomers}\n`;
    csvContent += `Khách hàng quay lại,${stats.returningCustomersCount}\n`;
    csvContent += `Tỷ lệ khách quay lại,${stats.returningCustomerRate.toFixed(2)}%\n\n`;

    // 2. Monthly Revenue
    csvContent += 'DOANH THU 6 THÁNG GẦN NHẤT\n';
    csvContent += 'Tháng,Doanh thu (VND),Tỷ lệ\n';
    monthlyData.forEach(item => {
      const raw = stats.monthlyRevenue?.find(r => r.month === item.month);
      const revVal = raw ? raw.revenue : 0;
      csvContent += `${item.month},${revVal},${item.val.toFixed(2)}%\n`;
    });
    csvContent += '\n';

    // 3. Booking types
    csvContent += 'PHÂN PHỐI LOẠI ĐẶT PHÒNG\n';
    csvContent += `Đoàn / Sự kiện (Group),${groupPct.toFixed(2)}%\n`;
    csvContent += `Đặt phòng trực tuyến (Online),${onlinePct.toFixed(2)}%\n`;
    csvContent += `Đặt phòng trực tiếp (Walk-in),${walkinPct.toFixed(2)}%\n`;

    // Download CSV file
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `Bao_cao_thong_ke_Elysian_${new Date().toISOString().slice(0, 10)}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6 animate-scale-in text-left">
      <div className="border-b border-slate-200 pb-4">
        <h3 className="text-slate-800 font-black text-base uppercase tracking-wider m-0">BÁO CÁO DOANH THU & HIỆU SUẤT</h3>
        <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Phân tích tình hình tài chính và doanh thu đặt phòng</p>
      </div>

      {/* Revenue grid charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Column chart with raw CSS */}
        <div className="lg:col-span-2 bg-white border border-slate-200 shadow-sm p-6">
          <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-100 pb-3 mb-6">
            Biểu đồ doanh thu 6 tháng gần nhất
          </h4>

          <div className="h-64 flex items-end justify-between gap-4 pt-4 px-2">
            {monthlyData.map((item, idx) => (
              <div key={idx} className="flex-1 flex flex-col items-center gap-2">
                <span className="text-[9px] text-primary font-black">{item.rev}</span>
                <div
                  className="w-full bg-primary hover:bg-slate-900 transition-all duration-300 shadow-md"
                  style={{ height: `${item.val * 1.8}px` }}
                />
                <span className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">{item.month}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Occupancy card */}
        <div className="bg-white border border-slate-200 shadow-sm p-6 flex flex-col justify-between">
          <div>
            <h4 className="text-xs font-black uppercase tracking-widest text-slate-800 border-b border-slate-100 pb-3 mb-4">Hiệu suất phòng nghỉ</h4>
            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider leading-relaxed">Phân phối tỷ lệ lấp đầy giữa các phân khúc khách hàng.</p>
          </div>

          <div className="space-y-4 py-4">
            <div>
              <div className="flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1 text-slate-600">
                <span>Đoàn / Sự kiện (Group)</span>
                <span className="text-slate-850 font-black">{groupPct.toFixed(0)}%</span>
              </div>
              <div className="w-full h-1.5 bg-slate-100">
                <div className="bg-primary h-full" style={{ width: `${groupPct}%` }}></div>
              </div>
            </div>

            <div>
              <div className="flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1 text-slate-600">
                <span>Đặt phòng trực tuyến (Online)</span>
                <span className="text-slate-850 font-black">{onlinePct.toFixed(0)}%</span>
              </div>
              <div className="w-full h-1.5 bg-slate-100">
                <div className="bg-blue-600 h-full" style={{ width: `${onlinePct}%` }}></div>
              </div>
            </div>

            <div>
              <div className="flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1 text-slate-600">
                <span>Đặt phòng trực tiếp (Walk-in)</span>
                <span className="text-slate-850 font-black">{walkinPct.toFixed(0)}%</span>
              </div>
              <div className="w-full h-1.5 bg-slate-100">
                <div className="bg-green-600 h-full" style={{ width: `${walkinPct}%` }}></div>
              </div>
            </div>
          </div>

          <div className="text-[9px] text-slate-500 font-bold uppercase tracking-wider border-t border-slate-100 pt-3">
            Dữ liệu được làm mới tự động mỗi 12 giờ.
          </div>
        </div>
      </div>

      {/* Metrics and Export row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Check-in/out Stats Card */}
        <div className="bg-white border border-slate-200 shadow-sm p-6 flex flex-col justify-between">
          <div>
            <h4 className="text-xs font-black uppercase tracking-widest text-slate-800 border-b border-slate-100 pb-3 mb-4">
              Thống kê Vận hành
            </h4>
            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider leading-relaxed">
              Tổng quan lượt nhận và trả phòng tại khách sạn.
            </p>
          </div>

          <div className="space-y-4 py-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-2">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-600">Tổng Check-in</span>
              <span className="text-sm font-black text-slate-850">{stats ? stats.totalCheckIns : 0} lượt</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-600">Tổng Check-out</span>
              <span className="text-sm font-black text-slate-850">{stats ? stats.totalCheckOuts : 0} lượt</span>
            </div>
          </div>

          <div className="text-[9px] text-slate-500 font-bold uppercase tracking-wider border-t border-slate-100 pt-3">
            Gồm các đơn đã hoàn thành hoặc đang lưu trú.
          </div>
        </div>

        {/* Customer Stats Card */}
        <div className="bg-white border border-slate-200 shadow-sm p-6 flex flex-col justify-between">
          <div>
            <h4 className="text-xs font-black uppercase tracking-widest text-slate-800 border-b border-slate-100 pb-3 mb-4">
              Khách hàng Thân thiết
            </h4>
            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider leading-relaxed">
              Tỷ lệ khách hàng quay lại lưu trú tại Elysian.
            </p>
          </div>

          <div className="py-2">
            <div className="flex justify-between items-baseline mb-2">
              <span className="text-2xl font-black text-slate-850">
                {stats ? stats.returningCustomerRate.toFixed(1) : '0.0'}%
              </span>
              <span className="text-[9px] text-slate-500 font-bold uppercase">Tỷ lệ quay lại</span>
            </div>
            <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden mb-4">
              <div 
                className="bg-primary h-full transition-all duration-500" 
                style={{ width: `${stats ? stats.returningCustomerRate : 0}%` }}
              ></div>
            </div>
            <div className="flex justify-between text-[9px] font-bold uppercase text-slate-500">
              <span>Tổng số khách: {stats ? stats.totalUniqueCustomers : 0}</span>
              <span>Khách quay lại: {stats ? stats.returningCustomersCount : 0}</span>
            </div>
          </div>

          <div className="text-[9px] text-slate-500 font-bold uppercase tracking-wider border-t border-slate-100 pt-3">
            Khách hàng đặt phòng từ 2 lần trở lên.
          </div>
        </div>

        {/* Export / Tools Card */}
        <div className="bg-white border border-slate-200 shadow-sm p-6 flex flex-col justify-between">
          <div>
            <h4 className="text-xs font-black uppercase tracking-widest text-slate-800 border-b border-slate-100 pb-3 mb-4">
              Xuất dữ liệu & Công cụ
            </h4>
            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider leading-relaxed">
              Tải báo cáo chi tiết về thiết bị cá nhân để quản lý.
            </p>
          </div>

          <div className="space-y-3 py-4">
            <button
              onClick={handleExportCSV}
              disabled={!stats}
              className="w-full py-3 bg-primary hover:bg-slate-900 text-white transition-colors duration-200 text-[10px] font-black uppercase tracking-widest border-none cursor-pointer flex items-center justify-center gap-2 shadow-md disabled:bg-slate-100 disabled:text-slate-400 disabled:cursor-not-allowed"
            >
              <span className="material-symbols-outlined text-sm">download</span>
              Xuất báo cáo (CSV)
            </button>
          </div>

          <div className="text-[9px] text-slate-500 font-bold uppercase tracking-wider border-t border-slate-100 pt-3">
            Định dạng tệp tin: CSV, hỗ trợ UTF-8.
          </div>
        </div>
      </div>
    </div>
  );
};

export default RevenueReports;
