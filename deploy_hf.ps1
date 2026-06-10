Write-Host "Waiting for Maven build to finish..."
# Wait until jar-with-dependencies is built and updated recently
Start-Sleep -Seconds 30

Write-Host "Updating tutorhub-sync version.json..."
cd D:\Ban_sao_du_an\temp_hf
git add version.json
Copy-Item "D:\Ban_sao_du_an\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" -Destination "update.jar" -Force
git add update.jar
git commit -m "Update client app 1.0.7"
git push

Write-Host "Updating tutorhub-core TutorServer.jar..."
cd D:\Ban_sao_du_an\temp_hf_core
Copy-Item "D:\Ban_sao_du_an\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" -Destination "TutorServer.jar" -Force
git add TutorServer.jar
git commit -m "Update client app 1.0.4"
git push

Write-Host "Deploy to Hugging Face successfully!"
