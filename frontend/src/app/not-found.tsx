import React from 'react';
import Link from 'next/link';
import { Shield, ArrowLeft } from 'lucide-react';

export default function NotFound() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="p-3 bg-sky-50 text-sky-600 rounded-2xl w-14 h-14 mx-auto mb-4 flex items-center justify-center border border-sky-100">
          <Shield className="w-8 h-8" />
        </div>
        <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">
          404
        </h1>
        <h2 className="text-base font-bold text-slate-800 mt-1">Page Not Found</h2>
        <p className="mt-2 text-xs text-slate-500 leading-relaxed max-w-sm mx-auto">
          The requested path does not exist or may have been moved.
        </p>

        <div className="mt-6">
          <Link
            href="/"
            className="btn-primary text-xs inline-flex items-center gap-1.5"
          >
            <ArrowLeft className="w-4 h-4" />
            Return to Homepage
          </Link>
        </div>
      </div>
    </div>
  );
}
