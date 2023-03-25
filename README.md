<img src="https://github.com/aurora-pl/resources/raw/main/aurora_800.png" width="10%" height="10%" align="left">

<h1 style="text-align: center;">The Aurora Programming Language</h1><br>

A dynamic ahead-of-time compiled programming language for general scripting and automation.

## An automation tool for the modern age.

<img src="https://github.com/aurora-pl/resources/raw/171834311f8be5b95f3e64d49e6536bdc10b1834/carbon(1).png" width="50%" align="left">

Automate tasks and parse text with ease. The Aurora Standard Library* provides you with all you need to get started.
```
fn parse_phone number
  pattern = "({}) {}-{}"
  parsed = match(number, pattern)
  return { parsed:0, parsed:1, parsed:2 }
end
```
<br clear="left">

## Safety in mind.

Aurora is completely memory-safe through it's automatic memory management. Aurora also has no concept of "null" - all functions must return a value, and non-returning functions can be marked as subroutines. Subroutines can only be called where their result is not used.

```
sub do_stuff
  for i, to(10)
    print i
  end
end

do_stuff
```

## Getting started

Aurora transpiles to C++, so a C++ compiler is required. The Aurora compiler is written in Rust.

`AuroraRuntime.h` must be in `/usr/include`, or wherever your compiler's include path is.

TO-DO: elaborate

*note: Aurora is in extreme alpha. The standard library barely exists currently.
