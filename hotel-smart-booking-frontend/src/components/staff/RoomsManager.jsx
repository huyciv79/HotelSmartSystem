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
  const [selectedTypeFilter, setSelectedTypeFilter] = useState('ALL');
  const [selectedFloorFilter, setSelectedFloorFilter] = useState('ALL');

  const defaultFloors = [1, 2, 3, 4, 5, 6];
  const dbFloors = Array.from(
    new Set(roomsList.map(r => r.floornumber !== undefined ? r.floornumber : r.floorNumber))
  ).filter(f => f !== undefined && f !== null);

  const displayFloors = Array.from(new Set([...defaultFloors, ...dbFloors])).sort((a, b) => a - b);

  const filteredRooms = roomsList.filter(room => {
    const roomNum = String(room.roomnumber || room.roomNumber || '');
    if (roomsSearchQuery && !roomNum.includes(roomsSearchQuery)) return false;

    const roomTypeObjId = room.roomtypeid || room.roomTypeId || (room.roomType && room.roomType.id);
    if (selectedTypeFilter !== 'ALL' && String(roomTypeObjId) !== String(selectedTypeFilter)) return false;

    const roomFloor = room.floornumber !== undefined ? room.floornumber : room.floorNumber;
    if (selectedFloorFilter !== 'ALL' && String(roomFloor) !== String(selectedFloorFilter)) return false;

    return true;
  });

  const getStatusStyles = (status) => {
    switch (status) {
      case 'Available':
        return {
          card: 'border-emerald-100 bg-emerald-50/30 hover:border-emerald-300',
          badge: 'bg-emerald-50 text-emerald-600 border border-emerald-100',
          label: 'Trống',
          dot: 'bg-emerald-400',
        };
      case 'Occupied':
        return {
          card: 'border-blue-100 bg-blue-50/30 hover:border-blue-300',
          badge: 'bg-blue-50 text-blue-600 border border-blue-100',
          label: 'Có khách',
          dot: 'bg-blue-400',
        };
      case 'Cleaning':
        return {
          card: 'border-amber-100 bg-amber-50/30 hover:border-amber-300',
          badge: 'bg-amber-50 text-amber-600 border border-amber-100',
          label: 'Dọn dẹp',
          dot: 'bg-amber-400',
        };
      default:
        return {
          card: 'border-slate-100 bg-slate-50/50 hover:border-slate-300',
          badge: 'bg-slate-100 text-slate-500 border border-slate-200',
          label: 'Bảo trì',
          dot: 'bg-slate-400',
        };
    }
  };

  return (
    <div className="space-y-6 animate-scale-in text-left">

      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h3 className="text-slate-800 font-black text-base uppercase tracking-wider m-0">Sơ đồ vận hành phòng nghỉ</h3>
          <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">
            Giao diện sơ đồ tầng trực quan hỗ trợ Lễ tân theo dõi trạng thái hoạt động
          </p>
        </div>

        <div className="relative min-w-[200px] w-full md:w-auto">
          <span className="material-symbols-outlined text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 text-sm pointer-events-none">search</span>
          <input
            type="text"
            placeholder="Tìm nhanh số phòng..."
            value={roomsSearchQuery}
            onChange={(e) => setRoomsSearchQuery(e.target.value)}
            className="w-full md:w-64 bg-slate-50 border border-slate-200/60 rounded-xl pl-9 pr-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
          />
        </div>
      </div>

      {/* Legend */}
      <div className="flex flex-wrap gap-3">
        {[
          { label: 'Trống', dot: 'bg-emerald-400' },
          { label: 'Có khách', dot: 'bg-blue-400' },
          { label: 'Dọn dẹp', dot: 'bg-amber-400' },
          { label: 'Bảo trì', dot: 'bg-slate-400' },
        ].map(item => (
          <span key={item.label} className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-500">
            <span className={`w-2 h-2 rounded-full ${item.dot}`} />
            {item.label}
          </span>
        ))}
      </div>

      {/* Filter + Floor + Grid */}
      <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] p-6 space-y-6">

        {/* Building label */}
        <div className="flex justify-between items-center border-b border-slate-100 pb-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary">domain</span>
            <span className="text-sm font-black text-slate-800 uppercase tracking-wider">Elysian Building</span>
          </div>
          <span className="text-[9px] text-slate-400 font-black uppercase tracking-widest bg-slate-50 px-3 py-1 border border-slate-100 rounded-lg">
            Tòa nhà trung tâm
          </span>
        </div>

        {/* Room Type Tabs */}
        <div className="space-y-2">
          <span className="block text-[9px] font-black uppercase tracking-widest text-slate-400">Phân loại hạng phòng:</span>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setSelectedTypeFilter('ALL')}
              className={`px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer rounded-xl border-none ${
                selectedTypeFilter === 'ALL'
                  ? 'bg-primary text-white shadow-[0_4px_12px_rgba(162,5,19,0.2)]'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
              }`}
            >
              Tất cả hạng phòng
            </button>
            {roomTypes.map(type => (
              <button
                key={type.id}
                onClick={() => setSelectedTypeFilter(type.id)}
                className={`px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer rounded-xl border-none ${
                  String(selectedTypeFilter) === String(type.id)
                    ? 'bg-primary text-white shadow-[0_4px_12px_rgba(162,5,19,0.2)]'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
                }`}
              >
                {type.name}
              </button>
            ))}
          </div>
        </div>

        {/* Floor Tabs */}
        <div className="space-y-2">
          <span className="block text-[9px] font-black uppercase tracking-widest text-slate-400">Chọn tầng làm việc:</span>
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => setSelectedFloorFilter('ALL')}
              className={`px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer rounded-xl border-none ${
                selectedFloorFilter === 'ALL'
                  ? 'bg-slate-800 text-white shadow-sm'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
              }`}
            >
              Tất cả tầng
            </button>

            {displayFloors.map(floor => (
              <button
                key={floor}
                onClick={() => setSelectedFloorFilter(floor)}
                className={`px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer rounded-xl border-none ${
                  String(selectedFloorFilter) === String(floor)
                    ? 'bg-slate-800 text-white shadow-sm'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
                }`}
              >
                Tầng {floor}
              </button>
            ))}
          </div>
        </div>

        {/* Room Grid */}
        <div className="border-t border-slate-100 pt-6">
          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-3">
            {filteredRooms.length === 0 ? (
              <div className="col-span-full py-12 text-center text-slate-400 font-bold uppercase tracking-wider">
                <span className="material-symbols-outlined text-4xl text-slate-200 block mb-3">sensor_door</span>
                Không tìm thấy phòng nào phù hợp với bộ lọc.
              </div>
            ) : (
              filteredRooms.map((room) => {
                const styles = getStatusStyles(room.status);
                const roomType = room.roomtypename || room.roomTypeName || (room.roomType && room.roomType.name) || 'Elysian Suite';
                const roomFloor = room.floornumber !== undefined ? room.floornumber : room.floorNumber;

                return (
                  <div
                    key={room.roomId || room.id}
                    className={`relative border ${styles.card} rounded-2xl p-4 flex flex-col justify-between items-center transition-all duration-300 group h-44 min-w-[120px] shadow-sm hover:shadow-md`}
                  >
                    {/* Status Badge + Floor */}
                    <div className="w-full flex justify-between items-center">
                      <span className={`text-[7px] font-black uppercase tracking-wider px-2 py-0.5 border rounded-md ${styles.badge}`}>
                        {styles.label}
                      </span>
                      <span className="text-[8px] text-slate-400 font-bold uppercase">T{roomFloor}</span>
                    </div>

                    {/* Room Number */}
                    <div className="flex flex-col items-center justify-center my-1.5">
                      <span className="text-[8px] text-slate-400 font-bold uppercase tracking-widest">Room no</span>
                      <span className="text-xl font-black text-slate-800 tracking-wide block leading-none mt-1">
                        {room.roomnumber || room.roomNumber}
                      </span>
                    </div>

                    {/* Room Type & Passcode */}
                    <div className="w-full text-center space-y-1">
                      <span className="text-[8px] font-extrabold uppercase tracking-wide text-slate-500 block line-clamp-2 leading-tight h-6 overflow-hidden">
                        {roomType}
                      </span>
                      <span className="text-[8px] font-mono tracking-widest text-slate-400 block bg-slate-50 py-1 border border-slate-100 rounded-lg">
                        {room.adminpasscode || room.adminPasscode || 'N/A'}
                      </span>
                    </div>

                    {/* Hover overlay */}
                    <div className="absolute inset-0 bg-white/95 rounded-2xl border border-slate-200 flex flex-col items-center justify-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10 p-4">
                      <span className="text-[9px] font-black uppercase tracking-wider text-slate-700 text-center truncate w-full">
                        {roomType}
                      </span>
                      <button
                        onClick={() => handleOpenEditRoomItem(room)}
                        className="w-full py-2 bg-primary hover:brightness-110 text-white text-[8px] font-black uppercase tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-1 rounded-lg"
                      >
                        <span className="material-symbols-outlined text-xs">edit</span>
                        Cập nhật
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
