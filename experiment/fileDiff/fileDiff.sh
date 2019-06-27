#! /bin/bash

rm ./src.txt
rm ./dst.txt

#for FILE in $1
find $1 -type f | while read FILE
do
	filename=$(basename $FILE)
	if [[ $filename =~ \.java$ ]] ;
	then
		echo $filename >> ./src.txt
	fi 
done

#for FILE in $2
find $2 -type f | while read FILE
do
	filename=$(basename $FILE)
	if [[ $filename =~ \.java$ ]] ;
	then
		echo $filename >> ./dst.txt
	fi 
done

diff ./src.txt ./dst.txt