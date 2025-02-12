####################################################################################################################################################################################################################################################################
#                                                                                                                                                                                                                                                                  #
#                                                                                                                                                                                                                                                                  #
#        Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)                                                                                                                                                                                                   #
#                                                                                                                                                                                                                                                                  #
#        This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.    #
#        This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.           #
#        You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.                                                                                                            #
#                                                                                                                                                                                                                                                                  #
####################################################################################################################################################################################################################################################################
#
from	__future__ import annotations
import	os, re, subprocess, tempfile
from	dataclasses import dataclass
from	typing import Any, Optional, Union
from	typing import IO, List
from	.exceptions import error
from	.id import IDs
#
__all__ = ['Crontab']
#
@dataclass
class Entry:
	line: str
	start: str
	command: str
	program: str
	comment: Optional[str]
	@classmethod
	def parse (cls, line: str) -> Entry:
		comment: Optional[str]
		try:
			(payload, comment) = line.split ('#', 1)
		except ValueError:
			(payload, comment) = (line, None)
		parts = payload.split (None, 5)
		return cls (
			line = line,
			start = ' '.join (parts[:5]),
			command = parts[5].strip (),
			program = parts[5].split ()[0],
			comment = comment
		)
	
	def concat (self) -> str:
		if self.comment:
			return f'{self.start} {self.command} #{self.comment}'
		return f'{self.start} {self.command}'

class Crontab (IDs):
	"""Handle crontabs

This helps handling of crontab entries. It tries to determinate
existing entries and tries to update these, add new entries and remove
obsolete entries."""
	__slots__ = ['superuser']
	header = re.compile ('# (DO NOT EDIT THIS FILE - edit the master and reinstall|\\([^ ]+ installed on|\\(Cron version)')
	enviroment = re.compile ('^[ \t]*[^= \t]+[ \t]*=')
	def __init__ (self) -> None:
		super ().__init__ ()
		self.superuser = os.getuid () == 0
	
	def update (self,
		crontab: List[str],
		user: Optional[str] = None,
		dryrun: bool = False,
		runas: Union[None, int, str] = None,
		remove: Optional[List[str]] = None
	) -> None:
		"""Update s a crontab entry
		
``crontab'' is a list of crontab lines using proper syntax for
crontabs, i.e. the first five elements are used to determinate the
starting time and the remaining part ist the command to start.
``user'' is the user to update the crontab for, if None then the
current user is used. If ``dryrun'' is False, then the crontab is
modified, otherwise it is only shown what would have been changed.
``runas'', if not None, is the user who should invoke this method. If
the user mismatches the current user, an exception is raised.
``remove'' is an entry or a list of entries to remove from the
crontab. Unknown entries are normally kept, as they may be added from
outside this process."""
		if runas is not None:
			pw = self.get_user (runas)
			if pw is None:
				raise error (f'Should run as user "{runas}" but this user is not known on this system')
			uid = os.getuid ()
			if pw.pw_uid != uid:
				cur = self.get_user (uid)
				raise error ('Should run as user "{runas}" but run as {current}'.format (
					runas = runas,
					current = str (uid) if cur is None else f'{cur.pw_name} ({cur.pw_uid})'
				))
		if user is not None and not self.superuser:
			raise error ('Need super user rights to modify user crontab')
		command = ['/usr/bin/crontab', '-l']
		if user is not None:
			command.append (f'-u{user}')
		pp = subprocess.Popen (command, stdin = subprocess.PIPE, stdout = subprocess.PIPE, stderr = subprocess.PIPE, text = True, errors = 'backslashreplace')
		(out, err) = pp.communicate ()
		if (pp.returncode != 0 or err) and not err.startswith ('no crontab'):
			raise error (f'Failed to read crontab with {pp.returncode}: {err}')
		out = out.strip ()
		#
		parsed = [Entry.parse (_l) for _l in crontab]
		#
		output: List[str] = []
		if out:
			for (lineno, line) in enumerate ([_o.strip () for _o in out.strip ().split ('\n')], start = 1):
				if lineno <= 3 and line.startswith ('#') and self.header.match (line) is not None:
					continue
				try:
					if line.strip ().startswith ('#'):
						raise ValueError ('comment line')
					if self.enviroment.match (line) is not None:
						raise ValueError ('enviroment setting')
					entry = Entry.parse (line)
					if not remove or os.path.basename (entry.program) not in remove:
						for p in parsed:
							if p.program == entry.program:
								if p.comment is None and entry.comment:
									p.comment = entry.comment
								if os.access (p.program, os.X_OK):
									output.append (p.concat ())
								parsed.remove (p)
								break
						else:
							output.append (line)
				except (IndexError, ValueError):
					output.append (line)
		for p in parsed:
			if os.access (p.program, os.X_OK):
				output.append (p.line)
		newtab = '\n'.join (output)
		if not dryrun:
			if newtab != out:
				command = ['/usr/bin/crontab']
				if not user is None:
					command.append (f'-u{user}')
				if not newtab:
					command.append ('-r')
					fd: Optional[IO[Any]] = None
				else:
					fd = tempfile.NamedTemporaryFile ()
					fd.write ((newtab + '\n').encode ('UTF-8'))
					fd.flush ()
					command.append (fd.name)
				pp = subprocess.Popen (command, stdout = subprocess.PIPE, stderr = subprocess.PIPE, text = True, errors = 'backslashreplace')
				(out, err) = pp.communicate ()
				if fd is not None:
					fd.close ()
				if pp.returncode or err:
					raise error (f'Failed to write crontab with {pp.returncode}: {err}')
		else:
			if newtab != out:
				print ('*** Crontab would be changed ***')
			else:
				print ('*** Crontab is unchanged ***')
			print (newtab)

