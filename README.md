<img src="https://github.com/aurora-pl/resources/raw/main/aurora_800.png" width="10%" height="10%" align="left">

<h1 style="text-align: center;">The Aurora Programming Language</h1><br>
A dynamically-type ahead-of-time compiled programming language for automation, general scripting, and small applications.
<br><br>
<img src="https://github.com/aurora-pl/resources/raw/171834311f8be5b95f3e64d49e6536bdc10b1834/carbon(1).png" width="60%" height="60%" align="left">

## A perl for the modern age.
Automate tasks and parse text with ease. The Aurora Standard Library* provides you with all you need to get started.
```
fn parse_phone number
  pattern = "({}) {}-{}"
  parsed = match(number, pattern)
  return { parsed:0, parsed:1, parsed:2 }
end
```
<br>

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

TO-DO: elaborate

*note: Aurora is in extreme alpha. The standard library barely exists currently.
