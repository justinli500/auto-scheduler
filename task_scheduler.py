#!/usr/bin/env python3
"""
Task Scheduler CLI - Automatically schedule tasks into Google Calendar
"""

import argparse
import pickle
import os.path
import yaml
from datetime import datetime, timedelta
from typing import List, Tuple, Optional, Dict, Any
from pathlib import Path
import tzlocal

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from dateutil import parser

# Google Calendar API scopes
SCOPES = ['https://www.googleapis.com/auth/calendar']

# Configuration defaults (used if no config file exists)
DEFAULT_WORK_START = 9  # 9 AM
DEFAULT_WORK_END = 17   # 5 PM
DEFAULT_MAX_SESSION = 4  # 4 hours max per session
DEFAULT_LOOKAHEAD_DAYS = 14  # Look 2 weeks ahead
DEFAULT_CALENDAR_ID = 'primary'
DEFAULT_CREDENTIALS_PATH = 'credentials.json'


def load_config(config_path: str = 'config.yaml') -> Dict[str, Any]:
    """
    Load configuration from YAML file

    Args:
        config_path: Path to config file

    Returns:
        Dictionary containing configuration values
    """
    # Default configuration
    default_config = {
        'calendar': {
            'id': DEFAULT_CALENDAR_ID,
            'credentials_path': DEFAULT_CREDENTIALS_PATH
        },
        'work_hours': {
            'start': DEFAULT_WORK_START,
            'end': DEFAULT_WORK_END,
            'include_weekends': False
        },
        'scheduling': {
            'max_session_hours': DEFAULT_MAX_SESSION,
            'lookahead_days': DEFAULT_LOOKAHEAD_DAYS
        },
        'defaults': {
            'description': ''
        }
    }

    # Try to load config file
    if os.path.exists(config_path):
        try:
            with open(config_path, 'r') as f:
                user_config = yaml.safe_load(f) or {}

            # Merge user config with defaults (user config takes precedence)
            for section, values in user_config.items():
                if section in default_config and isinstance(values, dict):
                    default_config[section].update(values)
                else:
                    default_config[section] = values

        except Exception as e:
            print(f"Warning: Could not load config file '{config_path}': {e}")
            print("Using default configuration.\n")

    return default_config


class TaskScheduler:
    """Main scheduler class for managing Google Calendar tasks"""

    def __init__(self, credentials_path: str = 'credentials.json'):
        """Initialize the scheduler with Google Calendar API"""
        self.credentials_path = credentials_path
        self.service = self._authenticate()
        # Get local timezone as IANA timezone name (e.g., 'America/Los_Angeles')
        self.local_tz_name = str(tzlocal.get_localzone())

    def _authenticate(self):
        """Authenticate with Google Calendar API"""
        creds = None
        token_path = Path.home() / '.task_scheduler_token.pickle'

        # Load existing credentials
        if token_path.exists():
            with open(token_path, 'rb') as token:
                creds = pickle.load(token)

        # Refresh or get new credentials
        if not creds or not creds.valid:
            if creds and creds.expired and creds.refresh_token:
                creds.refresh(Request())
            else:
                if not os.path.exists(self.credentials_path):
                    raise FileNotFoundError(
                        f"Credentials file not found at {self.credentials_path}. "
                        "Please download it from Google Cloud Console."
                    )
                flow = InstalledAppFlow.from_client_secrets_file(
                    self.credentials_path, SCOPES)
                creds = flow.run_local_server(port=0)

            # Save credentials
            with open(token_path, 'wb') as token:
                pickle.dump(creds, token)

        return build('calendar', 'v3', credentials=creds)

    def list_calendars(self):
        """List all available calendars"""
        try:
            calendars_result = self.service.calendarList().list().execute()
            calendars = calendars_result.get('items', [])

            if not calendars:
                print('No calendars found.')
                return

            print('\nAvailable Calendars:')
            print('-' * 80)
            for calendar in calendars:
                cal_id = calendar['id']
                cal_name = calendar.get('summary', 'Unnamed Calendar')
                is_primary = ' (PRIMARY)' if calendar.get('primary', False) else ''
                print(f"  {cal_name}{is_primary}")
                print(f"    ID: {cal_id}")
                print()

        except HttpError as error:
            print(f'An error occurred: {error}')

    def find_free_slots(
        self,
        start_date: datetime,
        end_date: datetime,
        calendar_id: str = 'primary',
        work_start_hour: int = DEFAULT_WORK_START,
        work_end_hour: int = DEFAULT_WORK_END
    ) -> List[Tuple[datetime, datetime]]:
        """
        Find free time slots in the calendar

        Args:
            start_date: Start of search period
            end_date: End of search period
            calendar_id: Calendar ID to search in
            work_start_hour: Start of work hours (24h format)
            work_end_hour: End of work hours (24h format)

        Returns:
            List of (start, end) tuples representing free slots
        """
        try:
            # Get all events in the time range
            events_result = self.service.events().list(
                calendarId=calendar_id,
                timeMin=start_date.isoformat() + 'Z',
                timeMax=end_date.isoformat() + 'Z',
                singleEvents=True,
                orderBy='startTime'
            ).execute()

            events = events_result.get('items', [])

            # Build list of busy periods
            busy_periods = []
            for event in events:
                start = event['start'].get('dateTime', event['start'].get('date'))
                end = event['end'].get('dateTime', event['end'].get('date'))

                # Parse the datetime strings
                start_dt = parser.parse(start)
                end_dt = parser.parse(end)

                # Make timezone-naive for comparison
                if start_dt.tzinfo:
                    start_dt = start_dt.replace(tzinfo=None)
                if end_dt.tzinfo:
                    end_dt = end_dt.replace(tzinfo=None)

                busy_periods.append((start_dt, end_dt))

            # Generate work hour slots and subtract busy periods
            free_slots = []
            current_date = start_date.replace(hour=0, minute=0, second=0, microsecond=0)

            while current_date < end_date:
                # Skip weekends (0 = Monday, 6 = Sunday)
                if current_date.weekday() < 5:  # Monday to Friday
                    work_start = current_date.replace(hour=work_start_hour, minute=0)
                    work_end = current_date.replace(hour=work_end_hour, minute=0)

                    # Check if this slot overlaps with any busy period
                    current_slot_start = work_start

                    for busy_start, busy_end in sorted(busy_periods):
                        # If there's free time before this busy period
                        if current_slot_start < busy_start and current_slot_start < work_end:
                            slot_end = min(busy_start, work_end)
                            if slot_end > current_slot_start:
                                free_slots.append((current_slot_start, slot_end))
                            current_slot_start = max(busy_end, current_slot_start)

                    # Add remaining time after last busy period
                    if current_slot_start < work_end:
                        free_slots.append((current_slot_start, work_end))

                current_date += timedelta(days=1)

            return free_slots

        except HttpError as error:
            print(f'An error occurred: {error}')
            return []

    def split_task(
        self,
        duration_hours: float,
        max_session_hours: float = DEFAULT_MAX_SESSION
    ) -> List[float]:
        """
        Split a task into multiple sessions if needed

        Args:
            duration_hours: Total duration of task
            max_session_hours: Maximum hours per session

        Returns:
            List of session durations in hours
        """
        if duration_hours <= max_session_hours:
            return [duration_hours]

        num_sessions = int(duration_hours / max_session_hours)
        remainder = duration_hours % max_session_hours

        sessions = [max_session_hours] * num_sessions
        if remainder > 0:
            sessions.append(remainder)

        return sessions

    def schedule_task(
        self,
        task_name: str,
        duration_hours: float,
        calendar_id: str = 'primary',
        description: str = '',
        lookahead_days: int = DEFAULT_LOOKAHEAD_DAYS,
        max_session_hours: float = DEFAULT_MAX_SESSION,
        work_start_hour: int = DEFAULT_WORK_START,
        work_end_hour: int = DEFAULT_WORK_END
    ) -> bool:
        """
        Schedule a task by finding free slots and creating calendar events

        Args:
            task_name: Name of the task
            duration_hours: How long the task will take
            calendar_id: Calendar ID to add events to
            description: Optional task description
            lookahead_days: How many days ahead to search
            max_session_hours: Maximum hours per work session
            work_start_hour: Start of work hours
            work_end_hour: End of work hours

        Returns:
            True if successfully scheduled, False otherwise
        """
        # Split task into sessions
        sessions = self.split_task(duration_hours, max_session_hours)

        print(f"\nScheduling '{task_name}' ({duration_hours}h total)")
        if len(sessions) > 1:
            print(f"Split into {len(sessions)} sessions: {sessions}")

        # Find free slots
        start_date = datetime.now()
        end_date = start_date + timedelta(days=lookahead_days)
        free_slots = self.find_free_slots(start_date, end_date, calendar_id, work_start_hour, work_end_hour)

        if not free_slots:
            print("❌ No free slots found in the next {} days".format(lookahead_days))
            return False

        # Schedule each session in the earliest available slots
        scheduled_sessions = []
        session_num = 1

        for session_duration in sessions:
            session_hours = int(session_duration)
            session_minutes = int((session_duration - session_hours) * 60)
            session_timedelta = timedelta(hours=session_hours, minutes=session_minutes)

            # Find first slot that can fit this session
            scheduled = False
            for slot_start, slot_end in free_slots:
                slot_duration = slot_end - slot_start

                if slot_duration >= session_timedelta:
                    # Schedule in this slot
                    event_start = slot_start
                    event_end = slot_start + session_timedelta

                    event = {
                        'summary': f"{task_name} (Session {session_num}/{len(sessions)})" if len(sessions) > 1 else task_name,
                        'description': description,
                        'start': {
                            'dateTime': event_start.isoformat(),
                            'timeZone': self.local_tz_name,
                        },
                        'end': {
                            'dateTime': event_end.isoformat(),
                            'timeZone': self.local_tz_name,
                        },
                        'reminders': {
                            'useDefault': True,
                        },
                    }

                    try:
                        created_event = self.service.events().insert(
                            calendarId=calendar_id,
                            body=event
                        ).execute()

                        scheduled_sessions.append((event_start, event_end))
                        print(f"✓ Session {session_num}: {event_start.strftime('%a %b %d, %I:%M %p')} - {event_end.strftime('%I:%M %p')} ({session_duration}h)")

                        # Remove or update this slot from free_slots
                        free_slots.remove((slot_start, slot_end))

                        # Add back any remaining time in this slot
                        if event_end < slot_end:
                            free_slots.append((event_end, slot_end))
                            free_slots.sort()

                        scheduled = True
                        session_num += 1
                        break

                    except HttpError as error:
                        print(f'❌ Error creating event: {error}')
                        return False

            if not scheduled:
                print(f"❌ Could not find slot for session {session_num} ({session_duration}h)")
                return False

        print(f"\n✓ Successfully scheduled all {len(sessions)} session(s)!")
        return True


def main():
    """Main CLI entry point"""
    # Load configuration from config file
    config = load_config()

    parser = argparse.ArgumentParser(
        description='Automatically schedule tasks into Google Calendar',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # List available calendars
  %(prog)s --list-calendars

  # Schedule a 3-hour task
  %(prog)s "Write report" 3

  # Schedule to a specific calendar
  %(prog)s "Write report" 3 --calendar "your-calendar@gmail.com"

  # Schedule with description
  %(prog)s "Study Python" 5 --description "Focus on async programming"

  # Custom work hours (8am-6pm)
  %(prog)s "Project work" 8 --work-hours 8 18

  # Limit session length to 2 hours
  %(prog)s "Research" 6 --max-session 2

  # Use a different config file
  %(prog)s "Task" 2 --config my-config.yaml
        """
    )

    parser.add_argument('task_name', nargs='?', help='Name of the task to schedule')
    parser.add_argument('duration', nargs='?', type=float, help='Duration in hours (e.g., 2.5)')
    parser.add_argument('--description', '-d',
                        default=config['defaults']['description'],
                        help='Task description')
    parser.add_argument('--calendar',
                        default=config['calendar']['id'],
                        help=f"Calendar ID to add events to (default from config: {config['calendar']['id']}). Use --list-calendars to see available calendars")
    parser.add_argument('--list-calendars', action='store_true',
                        help='List all available calendars and exit')
    parser.add_argument('--credentials', '-c',
                        default=config['calendar']['credentials_path'],
                        help=f"Path to Google credentials file (default from config: {config['calendar']['credentials_path']})")
    parser.add_argument('--config',
                        default='config.yaml',
                        help='Path to config file (default: config.yaml)')
    parser.add_argument('--lookahead', '-l', type=int,
                        default=config['scheduling']['lookahead_days'],
                        help=f"Days to look ahead (default from config: {config['scheduling']['lookahead_days']})")
    parser.add_argument('--max-session', '-m', type=float,
                        default=config['scheduling']['max_session_hours'],
                        help=f"Maximum hours per session (default from config: {config['scheduling']['max_session_hours']})")
    parser.add_argument('--work-hours', '-w', nargs=2, type=int,
                        metavar=('START', 'END'),
                        default=[config['work_hours']['start'], config['work_hours']['end']],
                        help=f"Work hours in 24h format (default from config: {config['work_hours']['start']} {config['work_hours']['end']})")

    args = parser.parse_args()

    try:
        scheduler = TaskScheduler(credentials_path=args.credentials)

        # Handle --list-calendars flag
        if args.list_calendars:
            scheduler.list_calendars()
            exit(0)

        # Validate required arguments for scheduling
        if not args.task_name or args.duration is None:
            parser.error('task_name and duration are required (unless using --list-calendars)')

        success = scheduler.schedule_task(
            task_name=args.task_name,
            duration_hours=args.duration,
            calendar_id=args.calendar,
            description=args.description,
            lookahead_days=args.lookahead,
            max_session_hours=args.max_session,
            work_start_hour=args.work_hours[0],
            work_end_hour=args.work_hours[1]
        )

        exit(0 if success else 1)

    except FileNotFoundError as e:
        print(f"❌ Error: {e}")
        exit(1)
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        exit(1)


if __name__ == '__main__':
    main()
