import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors, HttpInterceptorFn } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    const token = document.cookie.split('; ')
      .find(c => c.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=');
    if (token) {
      req = req.clone({ setHeaders: { 'X-XSRF-TOKEN': decodeURIComponent(token) } });
    }
  }
  return next(req);
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([csrfInterceptor]))
  ]
};
