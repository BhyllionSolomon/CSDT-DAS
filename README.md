# CSDT-DAS — Computing Science and Digital Technology Department Academic Results System

A results management and academic broadsheet system for the Department of
Computing Science and Digital Technology, covering four programmes: Cyber
Security, Information Technology, Software Engineering, and Computer Science.

## Stack

- Backend: Java 21, Spring Boot, Spring Data JPA, PostgreSQL
- Frontend: React, Vite

## Structure

CSDT-DAS-Student-Results/
- backend/     Spring Boot API
- frontend/    React admin dashboard

## Getting started

### Backend

cd backend/backend
./mvnw spring-boot:run

Runs on http://localhost:8081

### Frontend

cd frontend
npm install
npm run dev

## Features

- Student, course, department, level, programme, and academic session management
- Course-programme many-to-many mapping (shared and programme-specific courses)
- Result entry with CA + exam scoring, automatic grading, and an approval workflow
- Semester GPA and cumulative CGPA calculation, including correct handling of
  repeated/carryover courses (failed attempts are never removed from the
  cumulative record, matching the department's actual grading policy)
- Class of Degree classification
- Broadsheet export to Excel, Word, and PDF