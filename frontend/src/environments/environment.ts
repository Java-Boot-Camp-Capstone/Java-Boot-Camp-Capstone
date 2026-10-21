// Relative URLs: the same build works behind nginx (Docker / k3s) and `ng serve` (proxy.conf.json).
export const environment = {
  production: false,
  apiRoot: '/api',
  apiBaseUrl: '/api/v1'
};
