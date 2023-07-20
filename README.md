# MucticyclePipeline

Jon Woods; July 7, 2023

INSTRUCTION MANUAL

Step 1: Move the folder called PipelineSimulator out of this unzipped archive and into the Downloads category of your computer.

Step 2: Drag and drop the .txt PlainText file containing your MIPS instructions to the PipelineSimulator folder.

Step 3: Go through your text file and make sure that there is exactly ONE space between all values, a COMMA between each OPERAND of your arithmetic instructions (ADD, SUB, MUL_D, etc), and NO extra lines of non-MIPS between each instruction. Once all these conditions are met, save and close the file.

Step 4: Open a Linux platform on your computer, type in "cd ~", and then copy and paste the Pathname of the PipelineSimulator folder next to the tilde(~). Enter this full command to make PipelineSimulator the active directory.

Step 5: Enter the command "ls" to verify that both your text file and MulticyclePipeline.java are inside the directory and copy the name of the text file to your clipboard.

Step 6: Finally, enter the command "java MulticyclePipeline.java" to run the program. When prompted, paste the copied text file name, INCLUDING the ".txt" suffix, and press the enter key again to display the pipeline. If the pipeline of an instruction wraps onto extra lines, simply highlight the text from that instruction to the line containing the next one to verify which stages correspond to which instructions.
