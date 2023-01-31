export class CalendarError extends Error {
  constructor(status) {
    super()
    this.status = status
  }
}