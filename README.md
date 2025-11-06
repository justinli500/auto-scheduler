# Task Scheduler CLI

Automatically schedule work time for tasks into your Google Calendar based on task duration. The CLI finds free slots in your calendar, splits large tasks into manageable sessions, and prioritizes earlier time slots.

## Features

- **Smart scheduling**: Automatically finds free time slots in your calendar
- **Task splitting**: Breaks down large tasks into multiple work sessions
- **Avoids conflicts**: Respects existing calendar events
- **Prioritizes early slots**: Schedules tasks as soon as possible
- **Customizable work hours**: Define your working hours
- **Flexible session lengths**: Control maximum session duration

## Setup

### 1. Install Dependencies

```bash
cd task-scheduler
pip install -r requirements.txt
```

### 2. Set up Google Calendar API

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select an existing one)
3. Enable the Google Calendar API:
   - Navigate to "APIs & Services" > "Library"
   - Search for "Google Calendar API"
   - Click "Enable"
4. Create credentials:
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "OAuth client ID"
   - Choose "Desktop app" as the application type
   - Name it (e.g., "Task Scheduler CLI")
   - Click "Create"
5. Download the credentials:
   - Click the download icon next to your newly created OAuth 2.0 Client ID
   - Save the file as `credentials.json` in the `task-scheduler` directory

### 3. First Run Authentication

On your first run, the application will:
1. Open a browser window
2. Ask you to sign in to your Google account
3. Request permission to access your Google Calendar
4. Save the authentication token for future use

The token is stored at `~/.task_scheduler_token.pickle`

### 4. Configure Your Preferences (Optional)

The `config.yaml` file lets you set your default preferences so you don't have to specify them every time.

**First time setup:**
```bash
# Copy the example config to create your own
cp config.example.yaml config.yaml
```

**Then edit `config.yaml` to customize:**

```yaml
# Set your preferred calendar
calendar:
  id: "your-calendar-id@gmail.com"  # or "primary" for main calendar
  credentials_path: "credentials.json"

# Set your work hours
work_hours:
  start: 9   # 9 AM
  end: 17    # 5 PM

# Set scheduling preferences
scheduling:
  max_session_hours: 4.0  # Split tasks longer than 4 hours
  lookahead_days: 14      # Search 2 weeks ahead
```

**To find your calendar IDs:**
```bash
python task_scheduler.py --list-calendars
```

This will show all your calendars with their IDs. Copy the ID you want to use into `config.yaml`.

**Priority order:**
- Command-line arguments override config file settings
- Config file settings override built-in defaults
- If no config file exists, built-in defaults are used

## Usage

### Basic Usage

```bash
# Schedule a 3-hour task
python task_scheduler.py "Write report" 3

# Schedule a task with description
python task_scheduler.py "Study Python" 5 --description "Focus on async programming"
```

### Useful Commands

**List your calendars:**
```bash
python3 task_scheduler.py --list-calendars
```

**Schedule to a specific calendar:**
```bash
python3 task_scheduler.py "Meeting prep" 1 --calendar "work@gmail.com"
```

**Quick task scheduling (uses config.yaml defaults):**
```bash
python3 task_scheduler.py "Write documentation" 3
python3 task_scheduler.py "Code review" 1.5
python3 task_scheduler.py "Bug fixing" 2
```

**Deep work sessions (shorter sessions for better focus):**
```bash
# Break 8 hours into 2-hour blocks with breaks
python3 task_scheduler.py "Development sprint" 8 --max-session 2
```

**Flexible scheduling (night owl or different timezone):**
```bash
# Work from 2pm to 10pm
python3 task_scheduler.py "Late night coding" 4 --work-hours 14 22
```

**Academic/Study scheduling:**
```bash
python3 task_scheduler.py "Study for exam" 6 --max-session 1.5 --description "Computer Science final"
python3 task_scheduler.py "Assignment writeup" 4 --description "Due Friday"
```

**Project planning (look further ahead):**
```bash
# Schedule in the next month
python3 task_scheduler.py "Q1 planning" 12 --lookahead 30 --max-session 3
```

**Half-hour increments:**
```bash
python3 task_scheduler.py "Quick standup prep" 0.5
python3 task_scheduler.py "Email responses" 1.5
```

### Advanced Options

```bash
# Custom work hours (8am to 6pm)
python3 task_scheduler.py "Project work" 8 --work-hours 8 18

# Limit session length to 2 hours (good for focus/breaks)
python3 task_scheduler.py "Research" 6 --max-session 2

# Look further ahead (30 days instead of 14)
python3 task_scheduler.py "Long-term project" 10 --lookahead 30

# Custom credentials file location
python3 task_scheduler.py "Task" 2 --credentials /path/to/credentials.json

# Use different config file
python3 task_scheduler.py "Task" 2 --config work-config.yaml
```

### All Options

```
positional arguments:
  task_name             Name of the task to schedule
  duration              Duration in hours (e.g., 2.5)

optional arguments:
  -h, --help            Show help message
  --description, -d     Task description
  --credentials, -c     Path to Google credentials file (default: credentials.json)
  --lookahead, -l       Days to look ahead (default: 14)
  --max-session, -m     Maximum hours per session (default: 4)
  --work-hours, -w      Work hours in 24h format (default: 9 17)
```

## Examples

### Example 1: Simple Task
```bash
python task_scheduler.py "Review pull requests" 2
```

Output:
```
Scheduling 'Review pull requests' (2.0h total)
✓ Session 1: Mon Dec 30, 09:00 AM - 11:00 AM (2.0h)

✓ Successfully scheduled all 1 session(s)!
```

### Example 2: Large Task Split into Sessions
```bash
python task_scheduler.py "Complete project milestone" 10 --max-session 3
```

Output:
```
Scheduling 'Complete project milestone' (10.0h total)
Split into 4 sessions: [3.0, 3.0, 3.0, 1.0]
✓ Session 1: Mon Dec 30, 09:00 AM - 12:00 PM (3.0h)
✓ Session 2: Mon Dec 30, 01:00 PM - 04:00 PM (3.0h)
✓ Session 3: Tue Dec 31, 09:00 AM - 12:00 PM (3.0h)
✓ Session 4: Tue Dec 31, 01:00 PM - 02:00 PM (1.0h)

✓ Successfully scheduled all 4 session(s)!
```

### Example 3: Custom Work Schedule
```bash
# For night owls or different time zones
python task_scheduler.py "Deep work session" 4 --work-hours 14 22
```

## How It Works

1. **Authentication**: Connects to Google Calendar API using OAuth 2.0
2. **Find Free Slots**: Scans your calendar for available time during work hours (weekdays only)
3. **Split Task**: If task duration exceeds max session length, splits into multiple sessions
4. **Schedule**: Creates calendar events in the earliest available slots
5. **Avoid Conflicts**: Automatically works around existing calendar events

## Configuration

You can customize defaults in `config.yaml` or use command-line arguments to override them.

### Default Settings (if not configured)

- **Work hours**: 9 AM to 5 PM (Monday-Friday)
- **Max session length**: 4 hours
- **Lookahead period**: 14 days
- **Calendar**: Primary calendar
- **Weekend handling**: Automatically skips weekends

**To customize:** Edit `config.yaml` with your preferred values. See the Setup section above for details.

## Troubleshooting

### "Credentials file not found"
Make sure you've downloaded `credentials.json` from Google Cloud Console and placed it in the project directory.

### "No free slots found"
Try increasing the lookahead period with `--lookahead 30` or adjusting your work hours.

### Authentication issues
Delete the token file and re-authenticate:
```bash
rm ~/.task_scheduler_token.pickle
python task_scheduler.py "Test task" 1
```

## Tips & Best Practices

**Productivity:**
- Use shorter max session lengths (1.5-2 hours) for better focus and natural breaks
- Schedule deep work early in the day: `python3 task_scheduler.py "Focus work" 4 --max-session 2`
- Use 0.5 hour increments for quick tasks like email or admin work

**Organization:**
- Add descriptions to tasks for better context: `--description "Prepare slides for Monday meeting"`
- Use meaningful task names that make sense when you see them in your calendar
- Set your main calendar in `config.yaml` so you don't have to specify it each time

**Workflow:**
- Run `--list-calendars` once to find your calendar IDs, then save them in `config.yaml`
- Create multiple config files for different contexts (work-config.yaml, personal-config.yaml)
- Schedule recurring work by running the script multiple times with the same task name
- Use `--lookahead 30` at the start of each month to plan further ahead

**Time Management:**
- Schedule tasks right after planning meetings while requirements are fresh
- Block out deep work time before filling your calendar with meetings
- Use longer lookahead periods for big projects to ensure you have enough free slots

**Quick Commands:**
```bash
# Create an alias in your .bashrc or .zshrc for even faster scheduling
alias schedule='python3 ~/path/to/task_scheduler.py'

# Then just use:
schedule "Write code" 3
schedule "Review PRs" 1
```

## License

MIT License - Feel free to modify and use as needed!
