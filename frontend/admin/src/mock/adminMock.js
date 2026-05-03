export const mockUsers = [
  {
    id: "u-1001",
    username: "admin",
    nickname: "平台管理员",
    email: "admin@manifest.local",
    mobile: "13800000001",
    role: "平台管理员",
    status: "enabled",
  },
  {
    id: "u-1002",
    username: "tenant_ops",
    nickname: "企业操作员",
    email: "ops@manifest.local",
    mobile: "13800000002",
    role: "业务操作员",
    status: "enabled",
  },
  {
    id: "u-1003",
    username: "auditor",
    nickname: "审计员",
    email: "audit@manifest.local",
    mobile: "13800000003",
    role: "企业管理员",
    status: "disabled",
  },
];

export const mockBillsByUser = {
  "u-1001": [
    { blNo: "MRBL240001", vesselVoyage: "COSCO Star / 042E", pol: "Shanghai", pod: "Los Angeles", status: "已确认" },
    { blNo: "MRBL240002", vesselVoyage: "Ever Bloom / 118W", pol: "Ningbo", pod: "Hamburg", status: "待确认" },
  ],
  "u-1002": [
    { blNo: "MRBL240003", vesselVoyage: "OOCL Asia / 063E", pol: "Qingdao", pod: "Singapore", status: "解析中" },
  ],
  "u-1003": [
    { blNo: "MRBL240004", vesselVoyage: "Maersk Pearl / 221A", pol: "Xiamen", pod: "Rotterdam", status: "待确认" },
  ],
};

export const mockMarketDemands = [
  {
    id: 101,
    demandNo: "FD202605030001",
    publisherUserId: "u-1002",
    title: "上海到鹿特丹整柜运输",
    goodsName: "机械设备",
    departurePort: "SHANGHAI",
    destinationPort: "ROTTERDAM",
    demandStatus: "PENDING_REVIEW",
    auditStatus: "PENDING",
    createdAt: "2026-05-03T09:30:00",
  },
  {
    id: 102,
    demandNo: "FD202605030002",
    publisherUserId: "u-1003",
    title: "宁波到汉堡拼箱运输",
    goodsName: "家居样品",
    departurePort: "NINGBO",
    destinationPort: "HAMBURG",
    demandStatus: "PUBLISHED",
    auditStatus: "APPROVED",
    createdAt: "2026-05-03T10:05:00",
  },
  {
    id: 103,
    demandNo: "FD202605030003",
    publisherUserId: "u-1002",
    title: "青岛到新加坡冷链运输",
    goodsName: "冷冻食品",
    departurePort: "QINGDAO",
    destinationPort: "SINGAPORE",
    demandStatus: "REJECTED",
    auditStatus: "REJECTED",
    createdAt: "2026-05-03T10:25:00",
  },
];
