import { Component, input } from '@angular/core';

const paths: Record<string, string> = {
  grid: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  truck: 'M2 5h12v12H2z M14 9h4l4 4v4h-8 M5 17a2 2 0 1 0 4 0 M16 17a2 2 0 1 0 4 0',
  fleet: 'M3 6h13v9H3z M16 10h3l3 3v2h-6 M6 15v3 M18 15v3 M3 21h18',
  people:
    'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M17 4a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-3.87',
  route:
    'M6 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6 M18 22a3 3 0 1 0 0-6 3 3 0 0 0 0 6 M9 5h8a4 4 0 0 1 0 8H7a3 3 0 0 0 0 6h8',
  wrench: 'M14.7 6.3a5 5 0 0 0-6.4 6.4L2 19l3 3 6.3-6.3a5 5 0 0 0 6.4-6.4l-3 3-3-3z',
  clipboard: 'M9 4H5v18h14V4h-4 M9 2h6v4H9z M8 11h8 M8 16h6',
  chart: 'M4 3v18h17 M9 16v-5 M14 16V7 M19 16v-8',
  shield: 'M12 2l9 4v6c0 5-9 10-9 10S3 17 3 12V6z M8 12l3 3 5-6',
  lock: 'M5 10h14v12H5z M8 10V6a4 4 0 0 1 8 0v4 M12 15v3',
  key: 'M8 14a5 5 0 1 0 0-10 5 5 0 0 0 0 10 M12 12l9 9 M17 17l3-3 M19 19l3-3',
  logout: 'M9 3H3v18h6 M9 12h13 M17 7l5 5-5 5',
  arrow: 'M5 12h14 M13 6l6 6-6 6',
  refresh: 'M20 7a9 9 0 1 0 1 9 M20 2v6h-6',
  plus: 'M12 5v14 M5 12h14',
  calendar: 'M3 5h18v16H3z M7 2v6 M17 2v6 M3 10h18',
  clock: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20 M12 6v6l4 2',
  check: 'M20 6L9 17l-5-5',
  menu: 'M3 6h18 M3 12h18 M3 18h18',
  close: 'M6 6l12 12 M6 18L18 6',
};

@Component({
  selector: 'app-dashboard-icon',
  standalone: true,
  template: `<svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.65"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <path [attr.d]="path()" />
  </svg>`,
  styles: [
    ':host{display:inline-flex;width:20px;height:20px;flex-shrink:0}svg{width:100%;height:100%}',
  ],
})
export class DashboardIcon {
  readonly name = input('grid');
  path(): string {
    return paths[this.name()] ?? paths['grid'];
  }
}
