'use client';

import React from 'react';
import Link from 'next/link';
import { ShieldAlert, ArrowLeft, Building2 } from 'lucide-react';

export default function AccessDeniedPage() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="w-16 h-16 bg-rose-50 text-rose-600 rounded-2xl flex items-center justify-center mx-auto mb-4 border border-rose-200 shadow-sm">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
          403 — Access Denied
        </h1>
        <p className="mt-2 text-xs text-slate-500 leading-relaxed max-w-sm mx-auto">
          You do not have the required role or organization permission to access this resource.
          SecureGate enforces strict server-side tenant boundaries and role-based access control.
        </p>

        <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
          <Link
            href="/dashboard"
            className="btn-primary w-full sm:w-auto text-xs inline-flex items-center justify-center gap-1.5"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>
          <Link
            href="/organizations"
            className="btn-secondary w-full sm:w-auto text-xs inline-flex items-center justify-center gap-1.5"
          >
            <Building2 className="w-4 h-4" />
            Switch Organization
          </Link>
        </div>
      </div>
    </div>
  );
}
