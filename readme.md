# Save Soup by Angusiasty ![N|Solid](https://i.imgur.com/Wha8o59.jpg)

Save Soup is Windows console app for saving soup.io content.

#### Now supports NSFW soups 😎

To run the app you will need:
  - [Java 1.8+](https://www.java.com/en/download/)
  - [Google Chrome version 83](https://www.google.com/chrome/) - current version (11.07.2020)

# To run the app:

  - [Download app jar](https://drive.google.com/file/d/1HOOObq38P6pkItfo4UpipckladPpNuMa/view?usp=sharing) (or build it yourself 😉)
  - Open command prompt (press windows button and type cmd)
  - Go to jar file directory (I saved my jar file at 'C:\save_soup')
```sh
cd C:\save_soup
```
  - Start the jar file with parameters
```sh
java -jar save_soup.jar https://angusiasty.soup.io download 000000000 true false false
```
| Parameter | Accepted values |
| ------ | ------ |
| ```Soup path``` | https link to your soup |
| ```Download folder``` | name of the folder where your files will be saved |
| ```First file name``` | use ```000000000``` as default |
| ```Download images``` | ```true``` / ```false``` |
| ```Download videos``` | ```true``` / ```false``` |
| ```Download post text``` | ```true``` / ```false``` |

# In case of an error:
  It's possible to resume your download!
  
  Go to your download folder and open ```lastPage.txt``` file.
  
  Inside you will find a link to last visited page.
  
  Start the app like before, but:
  - change ```Soup path``` to path from ```lastPage.txt```
  - change ```First file name``` to number of your last downloaded file
