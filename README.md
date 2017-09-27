# Git Stack Expander

> Java Stack Trace + Git Commit ID = Useful Crash Report

![Screenshot](https://github.com/shannah/git-stack-expander/wiki/images/Screenshot2.png)

## Synopsis

Suppose you're given a stack trace of a crash that occurred in your app, but it originated from a slightly older version of your app.  The line numbers of this stack trace won't match up to the current state of your app's source code so it can be difficult to see how to fix the bug that caused the stack trace.  The solution is to check out the old revisions of your source code and compare the stack trace to that source code.  Doing this is tedious and time-consuming.

Git Stack Expander is a simple tool designed to solve this problem in a simpler way. You simply paste the stack trace into Git Stack Expander, optionally provide the Git commit ID corresponding to the stack trace, and it will expand the stack trace with the actual content that appears on each line of the stack trace.

## Requirements

* NodeJS
* Git

NOTE: Git must be in the CLI environment path.  On OS X it is like this by default.  On Windows, it will be in the path by default if you use the Git Bash (recommended). 

## Installation

~~~~
$ sudo npm install -g git-stack-expander
~~~~

## Features

* Converts stack trace into a hybrid stack trace + the lines of code that appears on each line of the stack trace at a given commit ID.
* Specify number of lines before and after each stack frame to display.
* Specify commit ID of any project in the workspace to load source for all projects at that point in time.
* Omit commit ID to simply display the source code at the local projects' current state.

## Usage instructions

Basic Usage:

~~~~
$ cd /path/to/workspace
$ git-stack-expander
~~~~

[See Wiki](https://github.com/shannah/git-stack-expander/wiki/Usage-Instructions)
