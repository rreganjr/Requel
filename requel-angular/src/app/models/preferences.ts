export interface UserPreferencesDto {
  sidebarProjectLimit: number;
  sidebarProjectStaleness: string;
}

export const STALENESS_OPTIONS = [
  { label: '1 Month', value: 'ONE_MONTH' },
  { label: '3 Months', value: 'THREE_MONTHS' },
  { label: '6 Months', value: 'SIX_MONTHS' },
  { label: '9 Months', value: 'NINE_MONTHS' },
  { label: '12 Months', value: 'TWELVE_MONTHS' },
  { label: 'Always Show', value: 'ALWAYS' },
];
