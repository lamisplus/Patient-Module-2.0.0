
export const url =
  process.env.NODE_ENV === "development"
    ? "http://localhost:8383/api/v1/"
    : "/api/v1/";

export const token =
  process.env.NODE_ENV === "development"
    ? "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJndWVzdEBsYW1pc3BsdXMub3JnIiwiYXV0aCI6IlN1cGVyIEFkbWluIiwibmFtZSI6Ikd1ZXN0IEd1ZXN0IiwiZXhwIjoxNzM3OTI3MDM2fQ.CMhCMIPTHSgkV2XJt1wsIZoN9peqoFEFuRVJCLSLU3I_lXKVtmSHP-w9BmKcY3GEPMxTkXByOrqBzMR8njVPFA"
    : new URLSearchParams(window.location.search).get("jwt");

