export const ROLES = ["BQT", "ACCOUNTANT", "RESIDENT", "AUTHORITY"];
export const canReadResidents = (roles = []) =>
  roles.some((r) => ["BQT", "ACCOUNTANT"].includes(r));
export const canManage = (roles = []) => roles.includes("BQT");
export const isResident = (roles = []) => roles.includes("RESIDENT");
export const homeForRoles = (roles = []) =>
  canReadResidents(roles)
    ? "/ho-gia-dinh"
    : isResident(roles)
      ? "/ho-cua-toi"
      : "/ho-so";
export const isSelf = (current, target) =>
  String(current?.id) === String(target?.id);
export const canAssignRoles = (current, target, roles) =>
  roles.length > 0 &&
  roles.every((r) => ROLES.includes(r)) &&
  new Set(roles).size === roles.length &&
  (!isSelf(current, target) || roles.includes("BQT")) &&
  (!roles.includes("RESIDENT") || Boolean(target.residentId));
