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
#
from	__future__ import annotations
import	os, re, subprocess, errno, logging
from	enum import Enum
from	typing import Any, Callable, Optional
from	typing import Dict, Generator, List, Set
from	.exceptions import error
from	.stream import Stream
from	.template import Template
#
logger = logging.getLogger (__name__)
#
__all__ = [
	'atoi', 'atob', 'calc_hash', 'sizefmt',
	'call', 'silent_call', 'match', 
	'listsplit', 'listjoin', 
	'Escape', 'escape', 'unescape',
	'Range', 'Config', 'Plugin',
	'Progress'
]
#
def atoi (s: Any, base: int = 10, default: int = 0) -> int:
	"""Lazy parses a value into an integer

parses input parameter as numeric value, use default if it is not
parsable.

>>> atoi (10)
10
>>> atoi ('A')
0
>>> atoi ('A', base = 16)
10
>>> atoi (3.14159)
3
>>> atoi (None)
0
>>> atoi (True)
1
>>> atoi (False)
0
>>> atoi (object ())
0
>>> atoi (object (), default = -1)
-1
"""
	if isinstance (s, int) or isinstance (s, float):
		return int (s)
	try:
		return int (s, base)
	except (ValueError, TypeError):
		return default

def atob (s: Any) -> bool:
	"""Interprets a value as a boolean

>>> atob (0)
False
>>> atob (1)
True
>>> atob (-1)
True
>>> atob (None)
False
>>> atob (3.14159)
True
>>> atob (0.0)
False
>>> atob (0.1)
True
>>> atob ('enabled')
True
>>> atob ('disabled')
False
>>> atob ('1')
True
>>> atob ('2')
False
>>> atob ('0')
False
>>> atob (object ())
False
>>> atob ([1, 2, 3])
False
"""
	if s is None:
		return False
	if isinstance (s, bool):
		return s
	if isinstance (s, int) or isinstance (s, float):
		return bool (s)
	if isinstance (s, str):
		return bool (s) and {
			'true': True,
			'enabled': True,
			'yes': True,
			'on': True,
			'false': False,
			'off': False,
			'no': False,
			'disabled': False
		}.get (s.lower (), s[:1] in {'1', '+'})
	return False

def calc_hash (s: str) -> int:
	"""Simple hash value generator

>>> calc_hash ('')
0
>>> calc_hash ('A')
65
>>> calc_hash ('Abc')
928115
>>> calc_hash ('agnitas.de')
468064859189097289471
>>> calc_hash (' ' * 200)
3459356304011571232903234033358723824931423566398556401012622596295213306933979148491444274326958590815363474846712218619683967266047508692094211384251125494722890670500752065384832639040151937319873047291700080457594364574425353940370233534086318223685841766026347041089066337484081114576380174284539979053600760549205271685617367429391303958596948422501611308711132779941001449034333819801239026197870385972527328
"""
	
	rc = 0
	for ch in s.encode ('UTF-8'):
		rc = (rc * 119) | ch
	return rc

def sizefmt (n: int, flip: int = 10) -> str:
	"""Converts the number n to a readable form of memory sizes.

Use flip to avoid a too rough rounding of the value, e.g.:
>>> sizefmt (1)
'1 Byte'
>>> sizefmt (8999)
'8999 Byte'
>>> sizefmt (8999, flip = 1)
'8.79 kByte'
>>> sizefmt (2 ** 24)
'16.00 MByte'
"""
	sizetab = [
		(1024 * 1024 * 1024 * 1024, 'TByte'),
		(1024 * 1024 * 1024, 'GByte'),
		(1024 * 1024, 'MByte'),
		(1024, 'kByte')
	]
	for (size, unit) in sizetab:
		if size * flip <= n:
			return '{value:.2f} {unit}'.format (value = float (n) / size, unit = unit)
	return f'{n} Byte'

def call (*args: Any, **kwargs: Any) -> int:
	"""Replacement for signal safe subprocess.call

while subprocess.call returns from wait, if a signal is receivied,
this version keep waiting."""
	pp = subprocess.Popen (*args, **kwargs)
	while True:
		try:
			return pp.wait ()
		except OSError as e:
			if e.args[0] == errno.ECHILD:
				return -1
	return 0

def silent_call (*args: str) -> int:
	with open (os.devnull, 'rb+') as fd:
		return call (args, stdin = fd, stdout = fd, stderr = fd)

def match (enum: Enum, **kwargs: Callable[[], Any]) -> Any:
	expect = set (enum.__class__.__members__.keys ())
	got = set (kwargs.keys ())
	if expect != got:
		cases = []
		missing = expect.difference (got)
		unexpected = got.difference (expect)
		if missing:
			cases.append ('missing expected keys: [{missing}]'.format (missing = ', '.join (sorted (missing))))
		if unexpected:
			cases.append ('unexpected keys: [{unexpected}]'.format (unexpected = ', '.join (sorted (unexpected))))
		raise error ('match {cases}'.format (cases = ', '.join (cases)))
	return kwargs[enum.name] ()

_pattern_listsplit = re.compile (',|,?[ \t\r\n]+')
def listsplit (s: Optional[str], maxsplit: int = 0, remove_empty: bool = True) -> Generator[str, None, None]:
	if s:
		for element in (_s.strip () for _s in _pattern_listsplit.split (s, maxsplit = maxsplit)):
			if not remove_empty or element:
				yield element

def listjoin (elements: Optional[List[str]], remove_empty: bool = True) -> str:
	if elements is not None:
		return ', '.join ([_e for _e in elements if _e])
	return ''

class Escape:
	__slots__ = ['escape_pattern', 'escape_replace', 'unescape_pattern', 'unescape_replace']
	def __init__ (self, escape_char: str, to_escape_chars: str) -> None:
		to_escape = f'{escape_char}{to_escape_chars}' if escape_char not in to_escape_chars else to_escape_chars
		self.escape_pattern = re.compile (f'[{to_escape}]')
		self.escape_replace: Dict[str, str] = Stream (to_escape).map (lambda ch: (ch, '{esc}{code:02X}'.format (esc = escape_char, code = ord (ch)))).dict ()
		self.unescape_pattern = re.compile (f'{escape_char}[0-9A-Z]{{2}}')
		self.unescape_replace = Stream (self.escape_replace.items ()).map (lambda kv: (kv[1], kv[0])).dict ()
	
	def escape (self, s: str) -> str:
		return self.escape_pattern.sub (lambda m: self.escape_replace[m.group ()], s)
	
	def unescape (self, s: str) -> str:
		return self.unescape_pattern.sub (lambda m: self.unescape_replace.get (m.group (), m.group ()), s)

_escape = Escape ('%', ',')
escape = _escape.escape
unescape = _escape.unescape

class Range:
	"""Create an interpreter for a positive numierc range expression

This class takes a generic expression, which is a comma separated list
of single expressions. Each single expression may be prefixed by an
exclamation mark to exclude these values.

>>> r = Range (None)
>>> 3 in r
False
>>> 0 in r
False
>>> -1 in r
False
>>> r = Range ('*')
>>> 3 in r
True
>>> 0 in r
True
>>> -1 in r
True
>>> r = Range ('!*')
>>> 3 in r
False
>>> 0 in r
False
>>> -1 in r
False
>>> r = Range ('10-30, 50-100')
>>> 0 in r
False
>>> 9 in r
False
>>> 10 in r
True
>>> 30 in r
True
>>> 31 in r
False
>>> 49 in r
False
>>> 50 in r
True
>>> 100 in r
True
>>> 101 in r
False
>>> r = Range ('10-30, !20')
>>> 9 in r
False
>>> 10 in r
True
>>> 30 in r
True
>>> 31 in r
False
>>> 20 in r
False
>>> 19 in r
True
>>> 21 in r
True
>>> r = Range ('!10-20', low = 1, high = 100)
>>> 1 in r
True
>>> 10 in r
False
>>> 20 in r
False
>>> 100 in r
True
>>> 0 in r
False
>>> 101 in r
False
"""
	__slots__ = ['slices', 'low', 'high', 'default', 'only_inverse']
	class Slice:
		"""Stores a single expression for Range"""
		__slots__ = ['inverse', 'start', 'end']
		def __init__ (self, inverse: bool, start: Optional[int], end: Optional[int]) -> None:
			self.inverse = inverse
			self.start = start
			self.end = end
		
		def __contains__ (self, val: int) -> bool:
			if (self.start is None or self.start <= val) and (self.end is None or val <= self.end):
				return True
			return False
		
		def __str__ (self) -> str:
			if self.start is not None and self.end is not None and self.start == self.end:
				s = str (self.start)
			else:
				def infinite (sign: str, value: Optional[int]) -> str:
					return str (value) if value is not None else f'{sign}oo'
				s = '{start}-{end}'.format (
					start = infinite ('-', self.start),
					end = infinite ('', self.end)
				)
			return '{inverse}{s}'.format (
				inverse = '!' if self.inverse else '',
				s = s
			)
	
	def __init__ (self, expr: Optional[str], low: Optional[int] = None, high: Optional[int] = None, default: bool = False) -> None:
		"""setup interpreter for range

the expression is a string represeantion of the desired range to
cover. Optional low and high values are used for open ranges (i.e. the
expression starts or ends with a dash). The default value is used, if
no single expression matched.
"""
		self.slices: List[Range.Slice] = []
		self.low = low
		self.high = high
		self.default = default
		self.only_inverse: Optional[bool] = None
		if expr:
			for e in listsplit (expr):
				if e.startswith ('!'):
					inverse = True
					e = e[1:].lstrip ()
					if self.only_inverse is None:
						self.only_inverse = True
				else:
					inverse = False
					self.only_inverse = False
				parts = [_e.strip () for _e in e.split ('-', 1)]
				try:
					if len (parts) == 2:
						start = int (parts[0]) if parts[0] else low
						end = int (parts[1]) if parts[1] else high
						if start is not None and end is not None and start > end:
							raise error (f'{expr}: start value ({start}) higher than end value ({end}) in {e}')
					elif e == '*':
						start = low
						end = high
					else:
						start = int (e)
						end = start
					slice = self.Slice (inverse, start, end)
					self.slices.append (slice)
				except ValueError:
					raise error (f'{expr}: invalid expression at: {e}')
			if self.only_inverse and low is not None and high is not None:
				self.slices.insert (0, self.Slice (False, low, high))

	def __contains__ (self, val: int) -> bool:
		if self.only_inverse:
			rc = (self.low is None or val >= self.low) and (self.high is None or val <= self.high)
		else:
			rc = self.default
		for slice in self.slices:
			if val in slice:
				rc = not slice.inverse
		return rc
	
	def __len__ (self) -> int:
		return len (self.slices)

	def __repr__ (self) -> str:
		return f'{self.__class__.__name__} ({self})'
		
	def __str__ (self) -> str:
		return (Stream (self.slices)
			.map (lambda s: str (s))
			.join (', ')
		)
	
	def mkset (self, low: Optional[int] = None, high: Optional[int] = None) -> Set[int]:
		"""Creates a set out of the parsed expression"""
		if low is None:
			low = self.low
			if low is None:
				raise error ('no low limit passed or set')
		if high is None:
			high = self.high
			if high is None:
				raise error ('no high limit passed or set')
		if self.only_inverse and (self.low is None or self.high is None):
			extra: List[Range.Slice] = [self.Slice (False, low, high)]
		else:
			extra = []
		rc: Set[int] = set ()
		if self.default:
			rc.update (range (low, high + 1))
		for slice in extra + self.slices:
			start = slice.start if slice.start is not None else low
			end = slice.end if slice.end is not None else high
			if not slice.inverse:
				rc.update (range (start, end + 1))
			else:
				rc = rc.difference (range (start, end + 1))
		return rc

class Config:
	"""Simple configuration

this class is a very limited version of agn3.config.Config to remove
the dependency to this class. It provides minimal set/get interface to
build up a required configuration for some classes. Use
agn3.config.Config if this class lacks a feature."""
	__slots__ = ['cfg']
	def __init__ (self, **kwargs: Any) -> None:
		self.cfg = kwargs
		
	def __getitem__ (self, key: str) -> Any:
		try:
			return self.cfg[key]
		except KeyError:
			return self.cfg[key.split ('.', 1)[-1]]
		
	def __setitem__ (self, key: str, value: Any) -> None:
		self.cfg[key] = value

	def __call__ (self, key: str, default: Any = None) -> Any:
		return self.get (key, default)
		
	def get (self, key: str, default: Any = None) -> Any:
		"""returns a configuratin value for ``key'', if available, else ``default''"""
		try:
			return self[key]
		except KeyError:
			return default
		
	def iget (self, key: str, default: int = 0) -> int:
		"""returns a configuratin value for ``key'' as an integer, if available, else ``default''"""
		try:
			return int (self[key])
		except (KeyError, ValueError, TypeError):
			return default
		
	def bget (self, key: str, default: bool = False) -> bool:
		"""returns a configuratin value for ``key'' as a boolean, if available, else ``default''"""
		try:
			return atob (self[key])
		except KeyError:
			return default

class Plugin:
	"""Simple plugin

This is a complete simple plugin if the whole feature and workload of the
full fledged plugin system is not required and the plugin code is provided
using strings. The usage is simple:

1.) create an instance of the class passing the code and optional name:

plugin = Plugin ('...')

2.) call a method from the plugin:

plugin.method ()

3.) done!

If the method is not found, a dummy method is called which does nothing
and returns None.

You can initial pass a prefilled namespace to enrich the namespace of the
running plugin.

During runtime, you can add global names into the namespace. To avoid
cluttering the namespace you first have to create a temporary context:
	
plugin ('push')

Then you can simple add or remove global names to/from the plugin:

plugin['someValue'] = 5
del plugin['someValue']
import sys
plugin['sys'] = sys

To remove the provided global names, simple dismiss the temporary context:

plugin ('pop')

Beware: you cannot overwrite existing names in the namespace!

You can subclass this class and implement the method "catchall" which must
return a function which takes a variable number of arguments. "catchall"
itself is only called with the name of the method which is not implemented
itself.
"""
	__slots__ = ['_ns', '_st', '_ca']
	def __init__ (self, code: str, name: Optional[str] = None, ns: Optional[Dict[str, Any]] = None) -> None:
		"""Create a plugin using ``code'' for ``name'' using namespace ``ns''"""
		self._ns = {} if ns is None else ns.copy ()
		self._st: List[Set[str]] = []
		if 'catchall' in self.__class__.__dict__ and callable (self.__class__.__dict__['catchall']):
			self._ca = self.catchall
		else:
			def catchall (name: str) -> Callable[..., Any]:
				def dummy (*args: Any, **kwargs: Any) -> Any:
					return None
				return dummy
			self._ca = catchall
		compiled = compile (code, name if name is not None else '*unset*', 'exec')
		exec (compiled, self._ns)
	
	def __call__ (self, command: str) -> None:
		"""Control interface for plugin class itself"""
		if command == 'push':
			self._st.append (set ())
		elif command == 'pop':
			if self._st:
				for token in self._st.pop ():
					del self._ns[token]

	def __setitem__ (self, option: str, value: Any) -> None:
		"""Set namespace content"""
		if self._st:
			ns = self._st[-1]
			if option not in self._ns or option in ns:
				self._ns[option] = value
				ns.add (option)
	
	def __delitem__ (self, option: str) -> None:
		"""Delete namespace content"""
		if self._st:
			ns = self._st[-1]
			if option in ns:
				del self._ns[option]
				ns.remove (option)

	def __getattr__ (self, name: str) -> Any:
		"""Call plugin method"""
		if name in self._ns and callable (self._ns[name]):
			return self._ns[name]
		return self._ca (name)

class Progress (Stream.Progress):
	"""Visiualize progress in logfile

For longer running processes or loops, this can be used to visualize
the progress of the process. For example when processing a CSV file:

rd = CSVReader ('some/file/name', CSVDefault)
p = dagent.Progress ('csv reader')
for row in rd:
	# process the row
	p ()
p.fin ()
rd.close ()"""
	__slots__ = ['name', 'display', 'template', 'ns', 'last', 'count']
	def __init__ (self, name: str, display: int = 10000, template: Optional[str] = None, ns: Optional[Dict[str, str]] = None) -> None:
		"""``name'' to be used as the name for log entries,
``display'' is the numeric value when reached a log entry is written,
``template'' can be set to a custom output template (using $count for
formated or $rawcount for unformated current count) and offering
``ns'' as a namespace for further customization."""
		self.name = name
		self.display = display
		self.template = Template (template if template is not None else 'Now at #$count')
		self.template.compile ()
		self.ns = ns.copy () if ns is not None else {}
		self.last = -1
		self.count = 0
		
	def show (self, final: bool = False) -> None:
		"""Write an entry to the logfile, if count has changed"""
		if self.count != self.last or (final and self.count == 0):
			self.ns['rawcount'] = str (self.count)
			self.ns['count'] = f'{self.count:,d}'
			self.log (self.template.fill (self.ns))
			self.last = self.count
			self.handle ()
	
	def __getitem__ (self, key: str) -> str:
		return self.ns[key]
	
	def __setitem__ (self, key: str, value: str) -> None:
		self.ns[key] = value
			
	def __call__ (self, inc: int = 1) -> int:
		"""Increment the counter, default by 1"""
		ocount = self.count
		self.count += inc
		if self.display and ocount // self.display != self.count // self.display:
			self.show ()
		return self.count
		
	def fin (self) -> int:
		"""Show final value, if not yet being showed"""
		self.show (True)
		return self.count
	#
	# Compatibility for Stream.progress
	def tick (self, elem: Any) -> None:
		self ()
	def final (self, count: int) -> None:
		self.fin ()
	#
	# can be overwritten by subclass
	def log (self, s: str) -> None:
		"""Log the output, can be overwritten"""
		logger.debug (f'{self.name}: {s}')
	
	def handle (self) -> None:
		"""Hook for more action, e.g. a database commit, when show() is invoked"""
		pass

