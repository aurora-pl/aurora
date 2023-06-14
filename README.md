<img src="https://github.com/aurora-pl/resources/raw/main/aurora_800.png" width="10%" height="10%" align="left">

<h1 style="text-align: center;">The Aurora Programming Language</h1><br>

A programming language for general scripting and automation.

## An automation tool for the modern age.

<img src="https://github.com/aurora-pl/resources/raw/171834311f8be5b95f3e64d49e6536bdc10b1834/carbon(1).png" width="50%" align="left">

Automate tasks and parse text with ease. The Aurora Standard Library provides you with all you need to get started.
```
fn parse_phone number
  pattern = "({}) {}-{}"
  parsed = match(number, pattern)
  return [ parsed:0, parsed:1, parsed:2 ]
end
```
<br clear="left">

## Safety in mind.

Aurora is completely memory-safe through it's automatic memory management. Aurora also has no concept of "null" - all functions must return a value, and non-returning functions can be marked as subroutines. Subroutines can only be called where their result is not used.

```
sub do_stuff
  10.times do i
    print i
  end
end

do_stuff
```

## Getting started

Download the latest .JAR artifact, or download and build yourself. Aurora uses the Intellij build system.
