@echo off
echo ===================================================
echo Pushing Ugam Waters clean codebase to GitHub...
echo ===================================================
cd /d "c:\Users\user\Desktop\Ugam website\mineral-water-temp\mineral-water-clean"

git init
git add .
git commit -m "Initialize Ugam Waters Clean codebase with customizations, profile validation, and payment fixes"
git branch -M main
git remote remove origin 2>nul
git remote add origin https://github.com/dattagunjal/Ugam_waters.git
git push -u origin main

echo ===================================================
echo Process complete.
echo ===================================================
pause
