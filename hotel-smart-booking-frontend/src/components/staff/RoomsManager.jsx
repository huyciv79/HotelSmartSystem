import { useState } from 'react';

const getRoomTypeAbbr = (name) => {
  if (!name) return 'RM';
  const cleanName = name.trim().toUpperCase();
  if (cleanName.includes('STANDARD')) return 'STD';
  if (cleanName.includes('DELUXE')) return 'DLX';
  if (cleanName.includes('PREMIUM')) return 'PRE';
  if (cleanName.includes('SUITE')) return 'SUI';
  if (cleanName.includes('FAMILY')) return 'FAM';
  if (cleanName.includes('EXECUTIVE')) return 'EXE';
  return cleanName.slice(0, 3);
};

const RoomsManager = ({
  roomsList,
  roomTypes,
  handleOpenEditRoomItem,
  fetchRealRooms,
  roomsSearchQuery,
  setRoomsSearchQuery
}) => {
  // Local filters matching the building/floor layout
  const [selectedTypeFilter, setSelectedTypeFilter] = useState('ALL');
  const [selectedFloorFilter, setSelectedFloorFilter] = useState('ALL');

  // Generate list of floors to display (always show floors 1-6 as mock base, and merge dynamically with any other floors in DB)
  const defaultFloors = [1, 2, 3, 4, 5, 6];
  const dbFloors = Array.from(
    new Set(
      roomsList.map(r => r.floornumber !== undefined ? r.floornumber : r.floorNumber)
    )
  ).filter(f => f !== undefined && f !== null);
  
  const displayFloors = Array.from(new Set([...defaultFloors, ...dbFloors])).sort((a, b) => a - b);

  // Filter rooms based on search, type, and floor
  const filteredRooms = roomsList.filter(room => {
    // Search query filter
    const roomNum = String(room.roomnumber || room.roomNumber || '');
    if (roomsSearchQuery && !roomNum.includes(roomsSearchQuery)) {
      return false;
    }

    // Type filter
    const roomTypeObjId = room.roomtypeid || room.roomTypeId || (room.roomType && room.roomType.id);
    if (selectedTypeFilter !== 'ALL' && String(roomTypeObjId) !== String(selectedTypeFilter)) {
      return false;
    }

    // Floor filter
    const roomFloor = room.floornumber !== undefined ? room.floornumber : room.floorNumber;
    if (selectedFloorFilter !== 'ALL' && String(roomFloor) !== String(selectedFloorFilter)) {
      return false;
    }

    return true;
  });

  const getStatusColor = (status) => {
    switch (status) {
      case 'Available':
        return {
          border: 'border-emerald-500/40 hover:border-emerald-500',
          bg: 'bg-emerald-950/20',
          text: 'text-emerald-400',
          glow: 'shadow-[0_0_15px_rgba(16,185,129,0.15)]',
          badge: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
        };
      case 'Occupied':
        return {
          border: 'border-blue-500/40 hover:border-blue-500',
          bg: 'bg-blue-950/20',
          text: 'text-blue-400',
          glow: 'shadow-[0_0_15px_rgba(59,130,246,0.15)]',
          badge: 'bg-blue-500/10 text-blue-400 border-blue-500/20'
        };
      case 'Cleaning':
        return {
          border: 'border-amber-500/40 hover:border-amber-500',
          bg: 'bg-amber-950/20',
          text: 'text-amber-400',
          glow: 'shadow-[0_0_15px_rgba(245,158,11,0.15)]',
          badge: 'bg-amber-500/10 text-amber-400 border-amber-500/20'
        };
      case 'Maintenance':
      default:
        return {
          border: 'border-neutral-700 hover:border-neutral-500',
          bg: 'bg-neutral-900/30',
          text: 'text-neutral-400',
          glow: 'shadow-none',
          badge: 'bg-neutral-800 text-neutral-400 border-neutral-700'
        };
    }
  };

  return (
    <div className="space-y-6 animate-scale-in text-left font-['Montserrat']">
      
      {/* Header section */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center border-b border-neutral-900 pb-4 gap-4">
        <div>
          <h3 className="text-white font-black text-base uppercase tracking-wider m-0">SƠ ĐỒ VẬN HÀNH PHÒNG NGHỈ</h3>
          <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">
            Giao diện sơ đồ tầng trực quan hỗ trợ Lễ tân theo dõi trạng thái hoạt động
          </p>
        </div>

        {/* Quick Search */}
        <div className="relative min-w-[200px] w-full md:w-auto">
          <input
            type="text"
            placeholder="Tìm nhanh số phòng..."
            value={roomsSearchQuery}
            onChange={(e) => setRoomsSearchQuery(e.target.value)}
            className="w-full md:w-64 bg-transparent border-b border-neutral-800 py-2 pr-8 font-bold text-xs outline-none text-white focus:border-primary placeholder:text-slate-600"
          />
          <span className="material-symbols-outlined text-slate-500 absolute right-0 top-1/2 -translate-y-1/2 text-sm pointer-events-none">
            search
          </span>
        </div>
      </div>

      {/* Building Header Card */}
      <div className="bg-[#0f0f12] border border-neutral-900 p-6 shadow-md space-y-6">
        
        {/* Building selector (Simulation of Building 01) */}
        <div className="flex justify-between items-center border-b border-neutral-900 pb-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary">domain</span>
            <span className="text-sm font-black text-white uppercase tracking-wider">Elysian Building</span>
          </div>
          <span className="text-[9px] text-slate-500 font-black uppercase tracking-widest bg-neutral-950 px-3 py-1 border border-neutral-850">
            Tòa nhà trung tâm
          </span>
        </div>

        {/* 1. ROOM TYPE TABS (Category tabs in screenshot) */}
        <div className="space-y-2">
          <span className="block text-[9px] font-black uppercase tracking-widest text-slate-500">
            Phân loại hạng phòng:
          </span>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setSelectedTypeFilter('ALL')}
              className={`px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border ${
                selectedTypeFilter === 'ALL'
                  ? 'bg-primary text-white border-primary shadow-lg shadow-primary/20'
                  : 'bg-neutral-900 hover:bg-neutral-800 text-slate-400 border-neutral-850'
              }`}
            >
              Tất cả hạng phòng
            </button>
            {roomTypes.map(type => (
              <button
                key={type.id}
                onClick={() => setSelectedTypeFilter(type.id)}
                className={`px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border ${
                  String(selectedTypeFilter) === String(type.id)
                    ? 'bg-primary text-white border-primary shadow-lg shadow-primary/20'
                    : 'bg-neutral-900 hover:bg-neutral-800 text-slate-400 border-neutral-850'
                }`}
              >
                {type.name}
              </button>
            ))}
          </div>
        </div>

        {/* 2. FLOOR TABS */}
        <div className="space-y-2 pt-2">
          <span className="block text-[9px] font-black uppercase tracking-widest text-slate-500">
            Chọn tầng làm việc:
          </span>
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => setSelectedFloorFilter('ALL')}
              className={`px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border ${
                selectedFloorFilter === 'ALL'
                  ? 'bg-white text-black border-white shadow-lg shadow-white/10'
                  : 'bg-neutral-900 hover:bg-neutral-800 text-slate-400 border-neutral-850'
              }`}
            >
              Tất cả tầng
            </button>
            
            {displayFloors.map(floor => (
              <button
                key={floor}
                onClick={() => setSelectedFloorFilter(floor)}
                className={`px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer border ${
                  String(selectedFloorFilter) === String(floor)
                    ? 'bg-white text-black border-white shadow-lg shadow-white/10'
                    : 'bg-neutral-900 hover:bg-neutral-800 text-slate-400 border-neutral-850'
                }`}
              >
                Tầng {floor}
              </button>
            ))}
          </div>
        </div>

        {/* 3. ROOM GRID CARDS (Floor Plan representation) */}
        <div className="border-t border-neutral-900 pt-6">
          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-4">

            {/* Room cards rendering */}
            {filteredRooms.length === 0 ? (
              <div className="col-span-full py-12 text-center text-slate-500 font-bold uppercase tracking-wider">
                <span className="material-symbols-outlined text-4xl block mb-2">sensor_door</span>
                Không tìm thấy phòng nào phù hợp với bộ lọc.
              </div>
            ) : (
              filteredRooms.map((room) => {
                const styles = getStatusColor(room.status);
                const roomType = room.roomtypename || room.roomTypeName || (room.roomType && room.roomType.name) || 'Elysian Suite';
                const roomFloor = room.floornumber !== undefined ? room.floornumber : room.floorNumber;

                return (
                  <div
                    key={room.roomId || room.id}
                    className={`relative border ${styles.border} ${styles.bg} ${styles.glow} p-4 flex flex-col justify-between items-center transition-all duration-300 group h-44 min-w-[125px]`}
                  >
                    {/* Status Badge */}
                    <div className="w-full flex justify-between items-center">
                      <span className={`text-[8px] font-black uppercase tracking-wider px-2 py-0.5 border ${styles.badge}`}>
                        {room.status === 'Available' ? 'Trống' : room.status === 'Occupied' ? 'Có khách' : room.status === 'Cleaning' ? 'Dọn dẹp' : 'Bảo trì'}
                      </span>
                      <span className="text-[8px] text-slate-500 font-bold uppercase">Tầng {roomFloor}</span>
                    </div>

                    {/* Room Number (Giant center text) */}
                    <div className="flex flex-col items-center justify-center my-1.5">
                      <span className="text-[8px] text-slate-500 font-bold uppercase tracking-widest">Room no</span>
                      <span className="text-xl font-black text-white tracking-wide block leading-none mt-1">
                        {room.roomnumber || room.roomNumber}
                      </span>
                    </div>

                    {/* Room Type Full Name & Admin Passcode */}
                    <div className="w-full text-center space-y-1">
                      <span className="text-[8px] font-extrabold uppercase tracking-wide text-slate-400 block line-clamp-2 leading-tight h-6 overflow-hidden">
                        {roomType}
                      </span>
                      <span className="text-[8px] font-mono tracking-widest text-slate-500 block bg-neutral-950/80 py-1 border border-neutral-900 rounded-sm">
                        Mật mã: {room.adminpasscode || room.adminPasscode || 'N/A'}
                      </span>
                    </div>

                    {/* Hover actions panel overlay */}
                    <div className="absolute inset-0 bg-[#0f0f12]/95 border border-primary/50 flex flex-col items-center justify-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10 p-4">
                      <span className="text-[9px] font-black uppercase tracking-wider text-slate-300 text-center truncate w-full">
                        {roomType}
                      </span>
                      <button
                        onClick={() => handleOpenEditRoomItem(room)}
                        className="w-full py-2 bg-primary hover:bg-primary/90 text-white text-[8px] font-black uppercase tracking-widest transition-colors cursor-pointer border-none flex items-center justify-center gap-1"
                      >
                        <span className="material-symbols-outlined text-xs">edit</span>
                        Cập nhật trạng thái
                      </button>
                    </div>
                  </div>
                );
              })
            )}

          </div>
        </div>

      </div>

    </div>
  );
};

export default RoomsManager;
